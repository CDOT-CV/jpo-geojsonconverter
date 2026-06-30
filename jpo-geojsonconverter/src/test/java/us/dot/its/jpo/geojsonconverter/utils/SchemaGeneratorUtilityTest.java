package us.dot.its.jpo.geojsonconverter.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SchemaGeneratorUtilityTest {

    private JsonNode invokeEnsureUtcTimeStampTsProperty(JsonNode schema) throws Exception {
        Method method = SchemaGeneratorUtility.class
                .getDeclaredMethod("ensureUtcTimeStampTsProperty", JsonNode.class);
        method.setAccessible(true);
        return (JsonNode) method.invoke(null, schema);
    }

    @Test
    public void run_withOutputArg_generatesSchemaAndReturnsZero() throws Exception {
        Path outputDir = Files.createTempDirectory("schema-generator-test-");
        int status = SchemaGeneratorUtility.run(new String[] {"--output", outputDir.toString()});

        assertEquals(0, status);

        File spatSchemaFile = outputDir.resolve("processed-spat.schema.json").toFile();
        assertTrue(spatSchemaFile.exists());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode schema = mapper.readTree(spatSchemaFile);
        JsonNode properties = schema.path("properties");
        JsonNode utcTimeStampTs = properties.path("utcTimeStampTS");

        assertTrue(utcTimeStampTs.isObject());
        assertEquals("string", utcTimeStampTs.path("type").asText());
        assertEquals("date-time", utcTimeStampTs.path("format").asText());
        assertFalse(properties.has("getUtcTimeStampTS()"));
        assertFalse(properties.has("class()"));
    }

    @Test
    public void run_withoutOutputValue_returnsOne() {
        int status = SchemaGeneratorUtility.run(new String[] {"--output"});

        assertEquals(1, status);
    }

    @Test
    public void run_withoutArgs_usesDefaultPathAndReturnsZero() throws Exception {
        Path defaultSchemaDir = Path.of("").toAbsolutePath().resolve("src/main/resources/schemas");
        int status = SchemaGeneratorUtility.run(new String[0]);

        assertEquals(0, status);

        File spatSchema = defaultSchemaDir.resolve("processed-spat.schema.json").toFile();
        assertTrue(spatSchema.exists());
    }

    @Test
    public void ensureUtcTimeStampTsProperty_addsPropertyWhenMissing() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        properties.putObject("utcTimeStamp").put("type", "string");

        JsonNode result = invokeEnsureUtcTimeStampTsProperty(schema);

        JsonNode utcTimeStampTs = result.path("properties").path("utcTimeStampTS");
        assertTrue(utcTimeStampTs.isObject());
        assertEquals("string", utcTimeStampTs.path("type").asText());
        assertEquals("date-time", utcTimeStampTs.path("format").asText());
    }

    @Test
    public void ensureUtcTimeStampTsProperty_keepsExistingProperty() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        ObjectNode properties = schema.putObject("properties");
        ObjectNode existing = properties.putObject("utcTimeStampTS");
        existing.put("type", "string");
        existing.put("format", "custom-format");

        JsonNode result = invokeEnsureUtcTimeStampTsProperty(schema);

        JsonNode utcTimeStampTs = result.path("properties").path("utcTimeStampTS");
        assertEquals("custom-format", utcTimeStampTs.path("format").asText());
    }

    @Test
    public void ensureUtcTimeStampTsProperty_returnsSameNodeWhenPropertiesMissing() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode schema = mapper.createObjectNode();
        schema.put("type", "object");

        JsonNode result = invokeEnsureUtcTimeStampTsProperty(schema);

        assertSame(schema, result);
        assertFalse(result.has("utcTimeStampTS"));
        assertNotNull(result);
    }
}

