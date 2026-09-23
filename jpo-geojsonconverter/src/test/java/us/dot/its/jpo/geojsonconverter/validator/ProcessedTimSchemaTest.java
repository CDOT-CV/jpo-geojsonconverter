package us.dot.its.jpo.geojsonconverter.validator;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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

    @Test
    void genericSignAndExitServiceContentTypesValidateAgainstProcessedTimSchema() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode processedTim = mapper.readTree(getTestJson(new ClassPathResource("json/sample.processed-tim.json")));
        JsonNode content = processedTim.at("/dataFrameFeatureCollection/features/0/properties/content");

        ((com.fasterxml.jackson.databind.node.ObjectNode) content).put("type", "GENERIC_SIGN");
        assertTrue(validator.validate(processedTim.toString()).isValid());

        ((com.fasterxml.jackson.databind.node.ObjectNode) content).put("type", "EXIT_SERVICE");
        assertTrue(validator.validate(processedTim.toString()).isValid());
    }
}
