package us.dot.its.jpo.geojsonconverter.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.bsm.ProcessedBsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.psm.ProcessedPsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.ProcessedRTCM;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.srm.ProcessedSrm;
import us.dot.its.jpo.geojsonconverter.pojos.spat.ProcessedSpat;
import us.dot.its.jpo.geojsonconverter.pojos.ssm.ProcessedSsm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SchemaGeneratorUtility {
    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        try {
            // Define the classes for which to generate schemas
            Class<?>[] targetClasses =
                    {ProcessedPsm.class, ProcessedBsm.class, ProcessedMap.class, ProcessedSpat.class, ProcessedRTCM.class,
                    ProcessedSrm.class, ProcessedSsm.class};

            ObjectMapper objectMapper = new ObjectMapper();

            SchemaGeneratorConfigBuilder configBuilder =
                    new SchemaGeneratorConfigBuilder(objectMapper, SchemaVersion.DRAFT_2020_12,
                            OptionPreset.PLAIN_JSON);

            configBuilder.with(new JacksonModule());

            configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES)
                    .with(Option.DEFINITIONS_FOR_ALL_OBJECTS)
                    .without(Option.GETTER_METHODS)
                    .without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                    .without(Option.NONSTATIC_NONVOID_NONGETTER_METHODS)
                    .without(Option.PUBLIC_NONSTATIC_FIELDS)
                    .with(Option.NONPUBLIC_NONSTATIC_FIELDS_WITH_GETTERS)
                    .without(Option.NONPUBLIC_NONSTATIC_FIELDS_WITHOUT_GETTERS);

            SchemaGenerator generator = new SchemaGenerator(configBuilder.build());

            // Find the project root directory
            Path currentPath = Paths.get("").toAbsolutePath();

            File resourcesDir = null;

            if (args.length > 0 && args[0].equals("--output")) {
                resourcesDir = new File(args[1]);
            } else {
                resourcesDir = new File(currentPath.toString(), "src/main/resources/schemas");
            }

            System.out.println("Creating schemas directory at: " + resourcesDir.getAbsolutePath());
            resourcesDir.mkdirs();

            // Generate schemas and save to files
            for (Class<?> targetClass : targetClasses) {
                JsonNode schema = generator.generateSchema(targetClass);

                // Explicitly include virtual SPaT timestamp computed property.
                if (ProcessedSpat.class.equals(targetClass)) {
                    schema = ensureUtcTimeStampTsProperty(schema);
                }

                // Create the schema file in the resources/schemas directory
                // Add hyphen after "Processed"
                String fileName = targetClass.getSimpleName().replaceAll("(?<=Processed)(?=\\w)", "-").toLowerCase()
                        + ".schema.json";
                File outputFile = new File(resourcesDir, fileName);
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, schema);

                System.out.println(
                        "Generated schema for: " + targetClass.getSimpleName() + " at " + outputFile.getAbsolutePath());
            }

            System.out.println("Schema generation completed successfully.");
            return 0;
        } catch (Exception e) {
            System.err.println("Error generating schemas: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }

    private static JsonNode ensureUtcTimeStampTsProperty(JsonNode schema) {
        if (!(schema instanceof ObjectNode schemaObject)) {
            return schema;
        }

        JsonNode propertiesNode = schemaObject.get("properties");
        if (!(propertiesNode instanceof ObjectNode propertiesObject)) {
            return schema;
        }

        if (!propertiesObject.has("utcTimeStampTS")) {
            ObjectNode utcTimeStampTs = propertiesObject.putObject("utcTimeStampTS");
            utcTimeStampTs.put("type", "string");
            utcTimeStampTs.put("format", "date-time");
        }

        return schemaObject;
    }
}
