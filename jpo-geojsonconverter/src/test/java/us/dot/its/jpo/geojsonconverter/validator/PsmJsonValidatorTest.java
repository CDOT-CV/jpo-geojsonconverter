package us.dot.its.jpo.geojsonconverter.validator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest({
    "valid.psm.json=classpath:json/valid.psm.json",
    "invalid.psm.json=classpath:json/invalid.psm.json",
    "spring.kafka.streams.auto-startup=false"})
@ActiveProfiles("test")
public class PsmJsonValidatorTest extends AbstractJsonValidatorTest {

    @Autowired
    private PsmJsonValidator psmJsonValidator;

    @Test
    public void psmJsonValidatorLoaded() {
        assertNotNull(psmJsonValidator);
    }

    @Test
    public void jsonSchemaResourceLoaded() {
        testJsonSchemaResourceLoaded(psmJsonValidator);
    }

    @Test
    public void jsonSchemaLoaded() throws IOException {
        testJsonSchemaLoaded(psmJsonValidator);
    }


    @Test
    public void validPsmJsonTest_String() {
        testJson(psmJsonValidator, validPsmJsonResource, true);
    }

    @Test
    public void invalidPsmJsonTest_String() {
        testJson(psmJsonValidator, invalidPsmJsonResource, false);
    }

    @Test
    public void validPsmJsonTest_ByteArray() {
        testJson_ByteArray(psmJsonValidator, validPsmJsonResource, true);
    }

    @Test
    public void invalidPsmJsonTest_ByteArray() {
        testJson_ByteArray(psmJsonValidator, invalidPsmJsonResource, false);
    }

    @Test
    public void testException() {
        SpatJsonValidator badValidator = new SpatJsonValidator(null);
        var result = badValidator.validate("invalid");
        assertFalse(result.isValid(), "An exception should have happened");
    }

    @Value("${valid.psm.json}")
    private Resource validPsmJsonResource;

    @Value("${invalid.psm.json}")
    private Resource invalidPsmJsonResource;
}
