package us.dot.its.jpo.geojsonconverter.converter.spat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.networknt.schema.Error;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.spat.DeserializedRawSpat;
import us.dot.its.jpo.geojsonconverter.pojos.spat.ProcessedSpat;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

public class SpatProcessedJsonConverterTest {
    SpatProcessedJsonConverter spatProcessedJsonConverter;
    OdeMessageFrameData spatMF;

    @BeforeEach
    void setup() throws IOException {
        String odeSpatJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/valid.spat.json")));
        try (JsonDeserializer<OdeMessageFrameData> odeSpatDeserializer =
                new JsonDeserializer<>(OdeMessageFrameData.class)) {
            spatMF = odeSpatDeserializer.deserialize("test-topic", odeSpatJsonString.getBytes());
        }
        spatProcessedJsonConverter = new SpatProcessedJsonConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(spatProcessedJsonConverter);
    }


    @Test
    public void testApplyValidation() {
        JsonValidatorResult validatorResults = new JsonValidatorResult();
        Exception exception = new Exception("test_exception");
        validatorResults.addException(exception);

        DeserializedRawSpat deserializedRawSpat = new DeserializedRawSpat();
        deserializedRawSpat.setOdeSpatMessageFrameData(spatMF);
        deserializedRawSpat.setValidatorResults(validatorResults);

        KeyValue<RsuIntersectionKey, ProcessedSpat> processedSpat = spatProcessedJsonConverter.apply(null, null);
        assertNotNull(processedSpat.key);
        assertEquals("ERROR", processedSpat.key.getRsuId());
        assertNull(processedSpat.value);
    }

    @Test
    public void testApplyFailure() {
        JsonValidatorResult validatorResults = new JsonValidatorResult();
        Exception exception = new Exception("test_exception");
        validatorResults.addException(exception);
        List<Error> validationMessages = new ArrayList<>();
        validatorResults.addValidationMessages(validationMessages);

        DeserializedRawSpat deserializedRawSpat = new DeserializedRawSpat();
        deserializedRawSpat.setValidationFailure(true);
        deserializedRawSpat.setValidatorResults(validatorResults);
        deserializedRawSpat.setFailedMessage("{");

        KeyValue<RsuIntersectionKey, ProcessedSpat> processedSpat =
                spatProcessedJsonConverter.apply(null, deserializedRawSpat);
        assertNotNull(processedSpat.key);
        assertNotNull(processedSpat.value);
        assertEquals("{", processedSpat.value.getValidationMessages().get(0).getMessage());
    }

    @Test
    public void testApplyException() {
        KeyValue<RsuIntersectionKey, ProcessedSpat> processedSpat = spatProcessedJsonConverter.apply(null, null);
        assertNotNull(processedSpat.key);
        assertEquals("ERROR", processedSpat.key.getRsuId());
        assertNull(processedSpat.value);
    }

}
