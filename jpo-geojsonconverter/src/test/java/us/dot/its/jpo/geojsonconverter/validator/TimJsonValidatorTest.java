package us.dot.its.jpo.geojsonconverter.validator;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;

class TimJsonValidatorTest {
    @Test
    void springUsesDefaultTimSchema() throws IOException {
        try (var context = new AnnotationConfigApplicationContext(TimJsonValidator.class)) {
            var validator = context.getBean(TimJsonValidator.class);
            assertEquals(new ClassPathResource("schemas/tim.schema.json"), validator.getJsonSchemaResource());
            assertNotNull(validator.getJsonSchema());
            try (var input = new ClassPathResource("json/sample.ode-tim.json").getInputStream()) {
                var result = validator.validate(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                assertTrue(result.isValid(), result::describeResults);
            }
            assertFalse(validator.validate("{}").isValid());
        }
    }

    @Test
    void directConstructionStillAcceptsCustomSchema() throws IOException {
        var validator = new TimJsonValidator("classpath:schemas/srm.schema.json");
        assertEquals(new ClassPathResource("schemas/srm.schema.json"), validator.getJsonSchemaResource());
        assertNotNull(validator.getJsonSchema());
        try (var input = new ClassPathResource("json/valid.srm.json").getInputStream()) {
            assertTrue(validator.validate(input.readAllBytes()).isValid());
        }
    }
}
