package us.dot.its.jpo.geojsonconverter.validator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

/**
 * Base class for Json Validator tests
 */
public abstract class AbstractJsonValidatorTest {

    protected void testJsonSchemaResourceLoaded(AbstractJsonValidator validator) {
        var resource = validator.getJsonSchemaResource();
        assertNotNull(resource);
        assertTrue(resource.exists(), "Resource does not exist");
    }

    protected void testJsonSchemaLoaded(AbstractJsonValidator validator) throws IOException {
        var jsonSchema = validator.getJsonSchema();
        assertNotNull(jsonSchema);
    }

    protected void testJson(AbstractJsonValidator validator, Resource resource, boolean expectValid) {
        assertNotNull(resource, "Couldn't get test json resource");
        var json = getTestJson(resource);
        JsonValidatorResult result = validator.validate(json);
        assertNotNull(result);

        assertEquals(result.isValid(), expectValid, "Validation result:%n%s".formatted(result.describeResults()));
        System.out.println(result.describeResults());
        
    }

    protected void testJson_ByteArray(AbstractJsonValidator validator, Resource resource, boolean expectValid) {
        assertNotNull(resource, "Couldn't get test json resource");
        byte[] jsonBytes = getTestJson_ByteArray(resource);
        JsonValidatorResult result = validator.validate(jsonBytes);
        assertNotNull(result);

        assertEquals(result.isValid(), expectValid, "Validation result:%n%s".formatted(result.describeResults()));
        System.out.println(result.describeResults());
    }

    
    
    protected String getTestJson(Resource jsonResource) {
        try (var is = jsonResource.getInputStream()) {
            return IOUtils.toString(is, StandardCharsets.UTF_8.name());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    protected byte[] getTestJson_ByteArray(Resource jsonResource) {
        try (var is = jsonResource.getInputStream()) {
            return IOUtils.toByteArray(is);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
