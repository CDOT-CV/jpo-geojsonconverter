package us.dot.its.jpo.geojsonconverter.validator;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest
    @ValueSource(strings = {"ADVISORY", "WORK_ZONE", "SPEED_LIMIT", "GENERIC_SIGN", "EXIT_SERVICE"})
    void contentChoicesValidateAgainstProcessedTimSchema(String contentType) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode processedTim = mapper.readTree(getTestJson(new ClassPathResource("json/sample.processed-tim.json")));
        JsonNode content = processedTim.at("/dataFrameFeatureCollection/features/0/properties/content");

        ((com.fasterxml.jackson.databind.node.ObjectNode) content).put("type", contentType);
        assertTrue(validator.validate(processedTim.toString()).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROAD_SIGNAGE", "COMMERCIAL_SIGNAGE"})
    void frameTypeLabelsAreRejectedAsContentTypes(String contentType) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode processedTim = mapper.readTree(getTestJson(new ClassPathResource("json/sample.processed-tim.json")));
        JsonNode content = processedTim.at("/dataFrameFeatureCollection/features/0/properties/content");

        ((com.fasterxml.jackson.databind.node.ObjectNode) content).put("type", contentType);
        assertFalse(validator.validate(processedTim.toString()).isValid());
    }
}
