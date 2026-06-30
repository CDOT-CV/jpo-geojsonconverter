package us.dot.its.jpo.geojsonconverter.validator;

import org.junit.jupiter.api.Test;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest({
        "valid.ssm.json=classpath:json/valid.ssm.json",
        "invalid.ssm.json=classpath:json/invalid.ssm.json",
        "spring.kafka.streams.auto-startup=false"})
@ActiveProfiles("test")
@Slf4j
public class SsmJsonValidatorTest extends AbstractJsonValidatorTest {

    @Autowired
    private SsmJsonValidator jsonValidator;

    @Test
    public void jsonValidatorLoaded() {
        assertNotNull(jsonValidator);
    }

    @Test
    public void jsonSchemaResourceLoaded() {
        testJsonSchemaResourceLoaded(jsonValidator);
    }

    @Test
    public void jsonSchemaLoaded() throws IOException {
        testJsonSchemaLoaded(jsonValidator);
    }

    @Test
    public void validJsonTest_String() {
        testJson(jsonValidator, validJsonResource, true);
    }

    @Test
    public void invalidJsonTest_String() {
        testJson(jsonValidator, invalidJsonResource, false);
    }

    @Test
    public void validJsonTest_ByteArray() {
        testJson_ByteArray(jsonValidator, validJsonResource, true);
    }

    @Test
    public void invalidJsonTest_ByteArray() {
        testJson_ByteArray(jsonValidator, invalidJsonResource, false);
    }

    @Test
    public void testException() {
        SsmJsonValidator badValidator = new SsmJsonValidator(null);

        log.debug("Class Path: {}", System.getProperty("java.class.path"));

        var result = badValidator.validate("invalid");
        assertFalse("An exception should have happened", result.isValid());
    }

    @Value("${valid.ssm.json}")
    private Resource validJsonResource;

    @Value("${invalid.ssm.json}")
    private Resource invalidJsonResource;
}
