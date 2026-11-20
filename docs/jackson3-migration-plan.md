# Jackson 3 Migration Plan

This document plans the move of the jpo-geojsonconverter (GJC) from Jackson 2 (`com.fasterxml.jackson`) to Jackson 3 (`tools.jackson`). It describes work for later; nothing here has been carried out yet.

## Why and when

Spring Boot 4 uses Jackson 3 by default and still supports Jackson 2 for compatibility. The GJC deserializes jpo-ode and ASN.1 POJO types, so it can only move once those libraries have moved. The ODE side is tracked in [CDOT-CV/jpo-ode#239](https://github.com/CDOT-CV/jpo-ode/pull/239).

**Start this work when all of these are true:**

1. jpo-ode#239 is merged, and a jpo-ode release (or pre-release) built on Jackson 3 is published.
2. The Jackson 3 build of the `jpo-asn-pojos` library is published and used by that jpo-ode release.
3. The jpo-conflictmonitor team has agreed on a schedule. The Conflict Monitor depends on the GJC artifact and uses its POJOs, serializers, deserializers and partitioners, so the GJC and Conflict Monitor migrations should be released together.

## Current footprint

Measured in November 2026, after the 4.0.0 dependency upgrades (Spring Boot 4.1.3, jpo-ode 7.0.0-alpha2):

| Item | Count / location |
|---|---|
| Main source files importing `com.fasterxml.jackson` | 92 |
| Test source files importing `com.fasterxml.jackson` | 11 |
| `com.fasterxml.jackson.annotation` imports | ~210. These **do not change**: Jackson 3 keeps the annotations package |
| `ObjectMapper` imports | 26 |
| Files using `JsonProcessingException` | 15 |
| `JavaTimeModule` uses | 2 (`DateJsonMapper`, `GeoJsonConverterApplication`) |
| Files calling `JsonNode.asText()` | 3 (all tests) |
| Shared mapper | `DateJsonMapper.getInstance()`. Used by `JsonSerializer`, `JsonDeserializer` and the `Processed*Deserializer`s, `RTCMDecoder`, and the `toString()` of the `Processed*` / `BaseFeature*` POJOs |
| Mappers created outside `DateJsonMapper` | `GeoJsonConverterApplication.jsonMapper()` (Spring bean), `pojos/geojson/GeoJSON` (`new ObjectMapper()`), `utils/SchemaGeneratorUtility`, `validator/AbstractJsonValidator` |

Libraries that are held on their last Jackson 2 compatible versions until this migration:

| Library | Held at | Jackson 3 version | Notable change |
|---|---|---|---|
| `com.github.victools:jsonschema-generator` (+ `-module-jackson`, `-module-jakarta-validation`) | 4.38.0 | 5.x | `JacksonModule` replaced by `JacksonSchemaModule`; uses `tools.jackson` types |
| `com.networknt:json-schema-validator` | 2.0.8 | 3.x | Same `SchemaRegistry` / `Schema` / `Error` API as 2.x, but takes `tools.jackson` `JsonNode` |
| `com.fasterxml.jackson.datatype:jackson-datatype-jsr310` | Boot-managed (2.21.8) | removed | Java time support is built into Jackson 3 |

## Migration approach

The ODE takes this approach in jpo-ode#239: a single mapper configuration built from `JsonMapper.builderWithJackson2Defaults()`, so that serialized output stays the same. The GJC should do the same, because its output topics are consumed by the Conflict Monitor, the CIMMS / ATSPM tooling and MongoDB sinks. **The JSON on the wire must not change.**

### Step 1: Consolidate mappers (still on Jackson 2)

This step can be done before the ODE is ready, and it makes the Jackson 3 change smaller.

- Make `DateJsonMapper` the single source of mapper configuration:
  - Java time module, `WRITE_DATES_AS_TIMESTAMPS=false`, `NON_NULL` inclusion and `FAIL_ON_UNKNOWN_PROPERTIES=false`.
  - Expose it as a builder method as well as the shared instance.
- Have the Spring `jsonMapper()` bean in `GeoJsonConverterApplication` use that configuration. Today it differs: it has no `NON_NULL` and no `FAIL_ON_UNKNOWN_PROPERTIES=false`. Confirm which behavior the REST and websocket endpoints rely on before unifying.
- Replace `new ObjectMapper()` in `GeoJSON`, `AbstractJsonValidator` and `SchemaGeneratorUtility` with the shared configuration, or document why each one needs plain defaults.
- Add **golden-file serialization tests**. For every output type (`ProcessedMap` GeoJSON and WKT, `ProcessedSpat`, `ProcessedBsm` (and `ProcessedBsmCollection`), `ProcessedPsm`, `ProcessedSrm`, `ProcessedSsm`, `ProcessedRTCM`, `ProcessedTim`, `ProcessedValidationMessage`), serialize a representative instance and assert byte-for-byte equality with a stored JSON fixture under `src/test/resources/json/golden/`. These fixtures are the regression net for Step 2.

### Step 2: Package and exception migration

Upgrade the jpo-ode dependency to the Jackson 3 release, then:

- Replace `com.fasterxml.jackson.core` / `databind` / `datatype` imports with `tools.jackson.core` / `tools.jackson.databind`. OpenRewrite's `org.openrewrite.java.jackson.UpgradeJackson_2_3` recipe can do the mechanical part. Leave `com.fasterxml.jackson.annotation` imports alone.
- Build `DateJsonMapper` with `JsonMapper.builderWithJackson2Defaults()`, then apply the GJC settings. Disable `MapperFeature.SORT_PROPERTIES_ALPHABETICALLY`, as the ODE does, to keep property order.
- Replace `JsonProcessingException` with `JacksonException`. It is unchecked in Jackson 3, so remove `throws` clauses that are no longer needed, but keep the try/catch blocks that log and return `null` in `JsonSerializer` and the deserializers. The partitioners rely on that behavior; see the comment in `RsuTimPartitioner`.
- Remove `JavaTimeModule` registration and the `jackson-datatype-jsr310` dependency.
- Rename `JsonNode.asText()` to `asString()`, and check any `fields()` / `elements()` iteration, which became `properties()` / `values()`.
- Spring: Boot 4 auto-configures a Jackson 3 `JsonMapper`. Make sure the GJC bean replaces it (same bean name and type), so REST and websocket output uses the GJC settings.

**Behavior differences to check explicitly** (the golden tests should catch them):

- Date and time serialization. Jackson 3 defaults `WRITE_DATES_AS_TIMESTAMPS` to `false`, and `ZonedDateTime` / `Instant` formatting can differ. The `utcTimeStamp` `Instant` field on `ProcessedSpat` matters for MongoDB.
- `FAIL_ON_NULL_FOR_PRIMITIVES` and `FAIL_ON_TRAILING_TOKENS` defaults.
- Property ordering, and `@JsonPropertyOrder` on GeoJSON types.
- `null` handling in `BaseFeature.properties` and `GeoJSON` geometry.
- Enum serialization of the J2735 and `Processed*` enum types.

### Step 3: Schema libraries

- Upgrade victools to 5.x, and replace `new JacksonModule()` with `new JacksonSchemaModule()` in `SchemaGeneratorUtility`.
- Upgrade networknt json-schema-validator to 3.x. Update `AbstractJsonValidator` and the validators to pass `tools.jackson` `JsonNode`s.
- Regenerate the schemas in `src/main/resources/schemas/` with `SchemaGeneratorUtility` and diff them against the committed versions. Any difference is a contract change that needs a schema version bump.

### Step 4: Remove Jackson 2

- Run `mvn dependency:tree -Dincludes=com.fasterxml.jackson.core` and confirm that only `jackson-annotations` remains, apart from transitive leftovers we have accepted.
- Delete any remaining Jackson 2 compatibility configuration.

## Suggested PR breakdown

| PR | Content | Can start before jpo-ode#239 merges? |
|---|---|---|
| A | Step 1: mapper consolidation and golden-file tests | Yes |
| B | Step 2: jpo-ode Jackson 3 version, package and exception migration | No |
| C | Step 3: victools 5 / networknt 3, plus schema regeneration and diff | No (after B) |
| D | Step 4: remove Jackson 2 leftovers, release notes | No (after C) |

## Verification

- `mvn clean verify` passes with the same test count as before, plus the new golden tests.
- The golden-file tests are unchanged between the commit before Step 2 and the commit after Step 3.
- The schema diff is empty, or any change is reviewed and comes with a version bump.
- A Conflict Monitor build against the migrated GJC snapshot passes its tests.
- An end-to-end run in the local docker-compose stack: send sample MAP / SPaT / BSM / PSM / TIM through the ODE with `scripts/tests/udpsender_*.py`, and compare the processed topic output with a Jackson 2 run.
- A Trivy scan of the jar or image shows no new HIGH/CRITICAL findings.
