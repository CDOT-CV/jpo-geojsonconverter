JPO GeoJSON Converter Release Notes
----------------------------


Version 4.0.0
----------------------------------------

### **Summary**

This release upgrades the build-time and runtime dependencies to versions that are actively supported, and fixes the known vulnerabilities in the libraries that ship in the application jar. Spring Boot moves to the 4.1 line, Kafka moves to the latest 3.9 patch, and the geospatial, JSON schema and test libraries are updated. Several vulnerable transitive libraries are removed or overridden. A Trivy scan of the application jar went from 100 findings (10 critical, 38 high) to none.

Enhancements in this release:

- Dependency upgrades: build tooling and Maven plugins
- Dependency upgrades: Spring Boot 4.1.3 and Kafka 3.9.2
- Dependency upgrades: libraries and vulnerable transitive dependencies

### **Dependency Updates**

| Dependency | Previous | New |
|---|---|---|
| Spring Boot (`spring-boot-starter-parent`) | 4.0.6 | 4.1.3 |
| Spring Framework (Boot-managed) | 7.0.7 | 7.0.11 |
| Spring for Apache Kafka (Boot-managed) | 4.0.5 | 4.1.3 |
| Apache Tomcat (Boot-managed) | 11.0.21 | 11.0.27 |
| Jackson 2 / Jackson 3 (Boot-managed) | 2.21.2 / 3.1.2 | 2.21.8 / 3.1.8 |
| Logback (Boot-managed) | 1.5.32 | 1.5.38 |
| Kafka Streams / Kafka Clients | 3.9.1 | 3.9.2 |
| JTS Core | 1.19.0 | 1.20.0 |
| GeoTools `gt-referencing` | 33.2 | 35.2 |
| networknt `json-schema-validator` | 2.0.1 | 2.0.8 |
| `json-unit-assertj` (test) | 5.0.0 | 6.4.0 |
| `snappy-java` (transitive, overridden) | 1.1.10.5 | 1.1.10.12 |
| `lz4-java` (transitive, overridden) | `org.lz4` 1.8.0 | `at.yawk.lz4` 1.11.4 |
| `org.json` (transitive, overridden) | 20231013 | 20260814 |
| JaCoCo Maven plugin | 0.8.14 | 0.8.16 |
| Sonar Maven plugin | 3.10.0.2594 | 5.9.0.7382 |
| `maven-jar-plugin` (`package-jar` profile) | 3.2.0 | Boot-managed (3.5.1) |
| Maven wrapper | Maven 3.9.9, wrapper 3.2.0 | Maven 3.9.16, wrapper 3.3.4 |
| Docker builder image | `maven:3.8-eclipse-temurin-21-alpine` | `maven:3.9-eclipse-temurin-21-alpine` |
| Devcontainer Maven / Kafka | 3.6.3 / 3.6.1 | 3.9.16 / 3.9.2 |

Removed from the runtime classpath:

- `jacoco-maven-plugin` was declared as a compile dependency, which brought in `plexus-utils` 3.0.24. It is now only used as a build plugin.
- `groovy-all` 3.0.8, together with `ant` 1.10.9 and `testng` 7.4.0, is now excluded. The jpo-ode parent pom adds it to every module, but neither jpo-ode nor the GeoJSON converter uses it. The application jar is about 20 MB smaller.

Intentionally not upgraded in this release:

- **Kafka 4.x**: Kafka stays on the 3.9 line to match the jpo-conflictmonitor, which uses this project's `StreamPartitioner` implementations. Kafka 4 changes that interface, so the move will be coordinated across both projects.
- **Jackson 3 libraries**: `jsonschema-generator` 5.x and `json-schema-validator` 3.x require Jackson 3. They will be adopted with the Jackson 3 migration, once jpo-ode moves to Jackson 3 ([CDOT-CV/jpo-ode#239](https://github.com/CDOT-CV/jpo-ode/pull/239)).
- **Spring Boot 4.2**: deferred until its first patch release.
- **Lombok**: held at 1.18.30.

### **Operational Notes**

- No Kafka broker changes are required. The client stays on the 3.9 line.
- Local builds need Maven 3.9 or later. `./mvnw` downloads Maven 3.9.16 automatically.
- The Docker builder stage now uses Maven 3.9. The runtime image (`eclipse-temurin:21-jre-alpine`) is unchanged.


Version 3.3.0
----------------------------------------

### **Summary**

This release focuses on improved configurability, validation accuracy, and message processing reliability. Key enhancements include configurable Kafka Streams settings, updated JSON schema validation, MAP validation fixes, and RTCM validation fixes. A bug related to timestamp handling in the SRM converter was fixed. An Instant timestamp field has been added to the ProcessedSpat message to allow for better compatability with MongoDB to support ATSPM integration in the CIMMS application.

Enhancements in this release:

- [Json schema validator updates](https://github.com/neaeraconsulting/jpo-geojsonconverter/pull/1)
- [Configurable streams config](https://github.com/neaeraconsulting/jpo-geojsonconverter/pull/2)
- [Fix bug with SRM timestamp handling](https://github.com/neaeraconsulting/jpo-geojsonconverter/pull/3/changes)
- [MAP ingress connections logic update](https://github.com/neaeraconsulting/jpo-geojsonconverter/pull/4)
- [Fix for RTCM Validation](https://github.com/neaeraconsulting/jpo-geojsonconverter/pull/6)
- [Update CI workflow and README badges](https://github.com/usdot-jpo-ode/jpo-geojsonconverter/pull/112)
- [Add UTC timestamp instant to ProcessedSpat](https://github.com/neaeraconsulting/jpo-geojsonconverter/pull/7)


Version 3.2.0
----------------------------------------

### **Summary**

This release adds full MessageFrame processing support to align with the reworked jpo-ode 5.0.2 update, enabling compatibility across BSM, SPaT, MAP, PSM, SRM, SSM, and RTCM message types. Kafka has been removed as a startup dependency to simplify local deployment and testing. Additional improvements include enhanced message processing reliability, refined time generation handling, and the introduction of independent schema versions for each processed message type.

Enhancements in this release:

- [CDOT PR 17](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/17): BSM MessageFrame Support
- [CDOT PR 18](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/18): SPaT MessageFrame Support
- [CDOT PR 19](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/19): Removes Kafka as a startup dependency for the GJC
- [CDOT PR 20](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/20): MapData MessageFrame Support
- [CDOT PR 21](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/21): PersonalSafetyMessage MessageFrame Support
- [CDOT PR 22](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/22): Full jpo-ode MessageFrame Support
- [CDOT PR 23](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/23): ProcessedRtcm support
- [CDOT PR 24](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/24): Processed MAP/SPAT/BSM/PSM adjustments
- [CDOT PR 26](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/26): Updating time generation logic to function with null DSecond
- [CDOT PR 27](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/27): Handling edge case where key is negative
- [CDOT PR 29](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/29): Processed SRM & SSM
- [CDOT PR 31](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/31): Processed SSM Serialization

Version 2.1.1 - HOTFIX
----------------------------------------

### **Summary**

This release includes a hotfix that resolved package build errors and reverted the POM version to 2.1.1 from 2.2.0.

Enhancements in this release:

- [USDOT PR 92](https://github.com/usdot-jpo-ode/jpo-geojsonconverter/pull/92): Processed PSM and Schema Generation

Version 2.1.0
----------------------------------------

### **Summary**

This release includes compatibility upgrades for the jpo-ode 4.1.0 version.

Enhancements in this release:

- [CDOT PR 13](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/13): Update gjc to work with latest version of the jpo-ode 4.1.0
- [CDOT PR 12](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/12): Adding Git history to CI Pipelines
- [CDOT PR 10](https://github.com/CDOT-CV/jpo-geojsonconverter/pull/10): Setting up CI with Azure Pipelines configuration.
- [USDOT PR 92](https://github.com/usdot-jpo-ode/jpo-geojsonconverter/pull/92): Processed PSM and Schema Generation

Version 2.0.0
----------------------------------------

### **Summary**

This release includes optimizations to the jpo-geojsonconverter's Kafka data production rates, a migration from the jpo-ode submodule to a GitHub Maven Central artifact, schema updates, ProcessedBsm processing logic modifications, and new support for the latest major version of the jpo-ode (4.0.0). Due to the support of the latest breaking changes of the jpo-ode, this version of the jpo-geojsonconverter is also incrementing a major version. OdeMapJson from the jpo-ode 4.0.0 will not be supported by versions of the jpo-geojsonconverter prior to 2.0.0. This also means OdeMapJson from earlier versions of the jpo-ode will not be supported by the jpo-geojsonconverter 2.0.0.

Enhancements in this release:

- USDOT PR84: Refactor output ProcessedBsm to be just a feature instead of a feature collection.
- CDOT PR5: Updated the processing logic of the ProcessedMap message to pull node values from the latest output of the OdeMapJson topic from the jpo-ode.
- USDOT PR80: Added GitHub action caching.
- USDOT PR78: Switched the artifact publisher to temurin.
- USDOT PR77: Modified the processing logic for the ProcessedBsm `timeStamp` value to pull from the OdeBsmJson `odeReceivedAt` value.
- USDOT PR76: Update schemas to JSON schema version `2020-12`.
- USDOT PR75: Remove repository submodules for the jpo-ode and add GitHub Maven Central artifact publishing.
- USDOT PR73: Added zstd compression to Kafka producers and consumers.

Version 1.4.0
----------------------------------------

### **Summary**

This release includes adding support for processing OdeBsmJson messages and creating ProcessedBsm. ProcessedBsm messages are geojson objects representing FeatureCollections with additional processed fields for additional context. ProcessedBsm currently only supports standard geojson format and not WKT format.

Enhancements in this release:

- PR67: Allow unknown properties via Jackson properties.
- PR66: Adds ProcessedBsm as a new message type that is created utilizing OdeBsmJson output from the jpo-ode.

Version 1.3.0
----------------------------------------

### **Summary**

This release includes adding a schema version to the ProcessedMap and ProcessedSpat message types to track changes in the schema.

Enhancements in this release:

- PR55: Adds schema version values to ProcessedMap and ProcessedSpat.

Version 1.2.0
----------------------------------------

### **Summary**

This release includes upgrading the project to use Java 21 in order to future proof the project.

Enhancements in this release:

- PR44: Java 21 support.

Version 1.1.0
----------------------------------------

### **Summary**

This release includes modifications to the ProcessedMap output, resiliency changes for Docker deployments and some minor bug fixes. The MAP and SPaT messages are now also partitioned in Kafka by their intersection ID rather than the origin IP.

Enhancements in this release:

- PR37: Partition Kafka topics by intersection ID instead of origin IP address.
- PR35: Fix bug with SPaT relative UTC timestamps when the hour rolls over.
- PR33: Include laneType field in ProcessedMap messages for each lane.
- PR27: Add auto restart policy to local docker-compose deployments.

Version 1.0.0
----------------------------------------

### **Summary**

The first release for the jpo-geojsonconverter, version 1.0.0, includes a fully functioning GeoJSON Converter for MAP messages and enhanced SPaT messages. The jpo-geojsonconverter consumes jpo-ode OdeMapJson and OdeSpatJson messages and creates ProcessedMap and ProcessedSpat messages. The jpo-geojsonconverter can use local Kafka or Confluent Cloud SASL authentication. Depending on the needs of the user, this release also provides the option to output all ProcessedMap message GeoJSON as well-known text (WKT) based geometry. Read more about the jpo-geojsonconverter in the [main README](<../README.md>).

Enhancements in this release:

- PR24: Revised log levels for project.
- PR23: Remove MAP node data and validation messages from WKT output.
- PR21: Well-Known Text (WKT) support for ProcessedMap messages.
- PR18: Kafka updates for creating topics on project creation for local Kafka.
- PR16: Schema updates to output processed messages.
- PR14: Confluent Cloud support as a Kafka broker using SASL authentication.
- PR13: Schema updates to MAP.
- PR12 and 13: MapGeoJson reworked to ProcessedMap messages. Contains relational information to support linking ProcessedSpat messages for visualization.
- PR11: SpatGeoJson reworked to ProcessedSpat messages with only enhanced data and no geoJSON.
- PR10: Added Maven JAR compiling plugin.
- PR8: Documentation added for JSON messages.
- PR7: MAP and SPaT OdeJson validation.
- PR5: First unit tests. Coverage >90%.
- PR3: MapGeoJson and SpatGeoJson output added.
- PR2: Initial Spring Boot application with Kafka Streams support and docker-compose for local deployment.
