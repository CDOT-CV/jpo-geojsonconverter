package us.dot.its.jpo.geojsonconverter.validator;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ProcessedTimSchemaTest extends AbstractJsonValidatorTest {

    private final AbstractJsonValidator validator = new AbstractJsonValidator("classpath:schemas/processed-tim.schema.json") {
    };

    @Test
    void sampleProcessedTimValidatesAgainstProcessedTimSchema() {
        testJson(validator, new ClassPathResource("json/sample.processed-tim.json"), true);
    }

    @Test
    void sampleProcessedTimWithCertificatePresentValidatesAgainstProcessedTimSchema() {
        testJson(validator, new ClassPathResource("json/sample.processed-tim-cert-present.json"), true);
    }
}
