package us.dot.its.jpo.geojsonconverter.validator;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Value;

@SpringBootTest({
    "valid.bsm.json=classpath:json/valid.bsm.json",
    "invalid.bsm.json=classpath:json/invalid.bsm.json",
    "spring.kafka.streams.auto-startup=false"
})
@ActiveProfiles("test")
public class BsmJsonValidatorTest extends AbstractJsonValidatorTest {

    @Autowired
    private BsmJsonValidator bsmJsonValidator;

    @Test
    public void bsmJsonValidatorLoaded() {
        assertNotNull(bsmJsonValidator);
    }

    @Test
    public void jsonSchemaResourceLoaded() {
        testJsonSchemaResourceLoaded(bsmJsonValidator);
    }

    @Test
    public void jsonSchemaLoaded() throws IOException {
        testJsonSchemaLoaded(bsmJsonValidator);
    }


    @Test
    public void validBsmJsonTest_String() {
        testJson(bsmJsonValidator, validBsmJsonResource, true);
    }

    @Test
    public void invalidBsmJsonTest_String() {
        testJson(bsmJsonValidator, invalidBsmJsonResource, false);
    }

    @Test
    public void validBsmJsonTest_ByteArray() {
        testJson_ByteArray(bsmJsonValidator, validBsmJsonResource, true);
    }

    @Test
    public void invalidBsmJsonTest_ByteArray() {
        testJson_ByteArray(bsmJsonValidator, invalidBsmJsonResource, false);
    }

    @Test
    public void testException() {
        SpatJsonValidator badValidator = new SpatJsonValidator(null);
        var result = badValidator.validate("invalid");
        assertFalse("An exception should have happened", result.isValid());
    }

    @Value("${valid.bsm.json}")
    private Resource validBsmJsonResource;

    @Value("${invalid.bsm.json}")
    private Resource invalidBsmJsonResource;
}
