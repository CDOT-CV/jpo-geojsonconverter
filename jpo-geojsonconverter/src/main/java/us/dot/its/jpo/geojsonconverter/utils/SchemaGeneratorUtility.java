package us.dot.its.jpo.geojsonconverter.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.victools.jsonschema.generator.*;
import com.github.victools.jsonschema.module.jackson.JacksonModule;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.bsm.ProcessedBsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.psm.ProcessedPsm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.ProcessedRTCM;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.srm.ProcessedSrm;
import us.dot.its.jpo.geojsonconverter.pojos.spat.ProcessedSpat;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.pojos.ssm.ProcessedSsm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SchemaGeneratorUtility {
    public static void main(String[] args) throws IOException {
        try {
            // Define the classes for which to generate schemas
            Class<?>[] targetClasses = {ProcessedPsm.class, ProcessedBsm.class, ProcessedMap.class, ProcessedSpat.class,
                    ProcessedRTCM.class, ProcessedSrm.class, ProcessedSsm.class, ProcessedTim.class};

            ObjectMapper objectMapper = new ObjectMapper();

            SchemaGeneratorConfigBuilder configBuilder =
                    new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2020_12, OptionPreset.PLAIN_JSON);

            // Add Jackson module for better handling of Jackson annotations
            configBuilder.with(new JacksonModule());

            // Add custom type resolver for Geometry polymorphic types
            configBuilder.forTypesInGeneral().withTypeAttributeOverride(new GeometryTypeResolver());

            // Add custom field resolver for geometry fields (handles generic type parameters)
            configBuilder.forFields().withInstanceAttributeOverride(new GeometryFieldResolver());

            // Configure additional options
            configBuilder.with(Option.EXTRA_OPEN_API_FORMAT_VALUES).without(Option.FLATTENED_ENUMS_FROM_TOSTRING)
                    .with(Option.DEFINITIONS_FOR_ALL_OBJECTS);

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
                SchemaGenerator generator = new SchemaGenerator(configBuilder.build());
                JsonNode schema = generator.generateSchema(targetClass);

                // Ensure Geometry types are included by manually adding their definitions
                // This avoids duplicate key errors from generating separate schemas
                JsonNode mainDefsNode = schema.get("$defs");
                com.fasterxml.jackson.databind.node.ObjectNode mainDefs;

                if (mainDefsNode == null) {
                    // Create $defs if it doesn't exist
                    mainDefs = JsonNodeFactory.instance.objectNode();
                    ((com.fasterxml.jackson.databind.node.ObjectNode) schema).set("$defs", mainDefs);
                } else {
                    mainDefs = (com.fasterxml.jackson.databind.node.ObjectNode) mainDefsNode;
                }

                // Add missing Geometry type definitions following the same pattern as Point
                // Each Geometry type has a base definition (-1) and a full definition (-2) with type discriminator
                addGeometryDefinitionIfMissing(mainDefs, "LineString", "array", 2);
                addGeometryDefinitionIfMissing(mainDefs, "Polygon", "array", 3);
                addGeometryDefinitionIfMissing(mainDefs, "MultiLineString", "array", 3);
                addGeometryDefinitionIfMissing(mainDefs, "MultiPolygon", "array", 4);

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
        } catch (Exception e) {
            System.err.println("Error generating schemas: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Exit successfully
        System.exit(0);
    }

    /**
     * Adds Geometry type definitions if they're missing from the schema. Follows the same pattern as Point: base
     * definition (-1) and full definition (-2) with type discriminator.
     */
    private static void addGeometryDefinitionIfMissing(com.fasterxml.jackson.databind.node.ObjectNode mainDefs,
            String geometryName, String coordinatesType, int coordinatesDepth) {
        String baseDefName = geometryName + "-1";
        String fullDefName = geometryName + "-2";

        // Add base definition if missing
        if (!mainDefs.has(baseDefName)) {
            com.fasterxml.jackson.databind.node.ObjectNode baseDef = JsonNodeFactory.instance.objectNode();
            baseDef.put("type", "object");
            com.fasterxml.jackson.databind.node.ObjectNode properties = JsonNodeFactory.instance.objectNode();

            // Add bbox property (same for all Geometry types)
            com.fasterxml.jackson.databind.node.ObjectNode bbox = JsonNodeFactory.instance.objectNode();
            bbox.put("type", "array");
            com.fasterxml.jackson.databind.node.ObjectNode bboxItems = JsonNodeFactory.instance.objectNode();
            bboxItems.put("type", "number");
            bboxItems.put("format", "double");
            bbox.set("items", bboxItems);
            properties.set("bbox", bbox);

            // Add coordinates property (varies by Geometry type)
            com.fasterxml.jackson.databind.node.ObjectNode coordinates = JsonNodeFactory.instance.objectNode();
            coordinates.put("type", "array");

            // Build nested array structure based on depth
            com.fasterxml.jackson.databind.node.ObjectNode currentLevel = coordinates;
            for (int i = 0; i < coordinatesDepth - 1; i++) {
                com.fasterxml.jackson.databind.node.ObjectNode items = JsonNodeFactory.instance.objectNode();
                items.put("type", "array");
                currentLevel.set("items", items);
                currentLevel = items;
            }

            // Final level: array of numbers
            com.fasterxml.jackson.databind.node.ObjectNode finalItems = JsonNodeFactory.instance.objectNode();
            finalItems.put("type", "number");
            finalItems.put("format", "double");
            currentLevel.set("items", finalItems);

            properties.set("coordinates", coordinates);
            baseDef.set("properties", properties);
            mainDefs.set(baseDefName, baseDef);
        }

        // Add full definition with type discriminator if missing
        if (!mainDefs.has(fullDefName)) {
            com.fasterxml.jackson.databind.node.ObjectNode fullDef = JsonNodeFactory.instance.objectNode();
            fullDef.set("$ref", JsonNodeFactory.instance.textNode("#/$defs/" + baseDefName));
            fullDef.put("type", "object");
            com.fasterxml.jackson.databind.node.ObjectNode properties = JsonNodeFactory.instance.objectNode();
            com.fasterxml.jackson.databind.node.ObjectNode typeProp = JsonNodeFactory.instance.objectNode();
            typeProp.put("const", geometryName);
            properties.set("type", typeProp);
            fullDef.set("properties", properties);
            com.fasterxml.jackson.databind.node.ArrayNode required = JsonNodeFactory.instance.arrayNode();
            required.add("type");
            fullDef.set("required", required);
            mainDefs.set(fullDefName, fullDef);
        }
    }
}
