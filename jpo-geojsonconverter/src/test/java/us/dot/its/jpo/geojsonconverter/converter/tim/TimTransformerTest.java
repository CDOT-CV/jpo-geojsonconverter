package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.kafka.streams.KeyValue;
import org.junit.Before;
import org.junit.Test;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.pojos.common.DeserializedRawMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

public class TimTransformerTest {
    private TimTransformer timTransformer;
    private OdeMessageFrameData timMF;

    @Before
    public void setup() throws IOException {
        // Load sample TIM JSON file
        String timJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/sample.ode-tim.json")));

        try (JsonDeserializer<OdeMessageFrameData> odeTimDeserializer =
                new JsonDeserializer<>(OdeMessageFrameData.class)) {
            timMF = odeTimDeserializer.deserialize("test-topic", timJsonString.getBytes());
        }

        TimGeometryProcessor geometryProcessor = new TimGeometryProcessor();
        TimConverter timConverter = new TimConverter(geometryProcessor);
        timTransformer = new TimTransformer(timConverter);
    }

    @Test
    public void testTransformWithValidTim() {
        // Test successful TIM transformation
        DeserializedRawMessageFrame deserializedRawTim = new DeserializedRawMessageFrame();
        deserializedRawTim.setOdeMessageFrameData(timMF);
        deserializedRawTim.setValidationFailure(false);
        deserializedRawTim.setValidationResults(new JsonValidatorResult());

        KeyValue<RsuTimKey, ProcessedTim> result = timTransformer.transform(null, deserializedRawTim);

        // Verify transformation result
        assertNotNull(result);
        assertNotNull(result.key);
        assertNotNull(result.value);

        // Verify key properties
        RsuTimKey key = result.key;
        assertNotNull(key.getRsuId());
        assertEquals("18D4A500000D7BA133", key.getPacketId());
        assertEquals(1, key.getMsgCnt().intValue());

        // Verify processed TIM properties
        ProcessedTim processedTim = result.value;
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getOdeReceivedAt());
        assertEquals(1, processedTim.getMsgCnt().intValue());
        assertEquals("18D4A500000D7BA133", processedTim.getPacketId());
    }

    @Test
    public void testTransformWithValidationFailure() {
        // Test TIM transformation with validation failure
        JsonValidatorResult validatorResult = new JsonValidatorResult();
        Exception testException = new Exception("Critical validation error");
        validatorResult.addException(testException);

        DeserializedRawMessageFrame deserializedRawTim = new DeserializedRawMessageFrame();
        deserializedRawTim.setOdeMessageFrameData(timMF);
        deserializedRawTim.setValidationFailure(true);
        deserializedRawTim.setValidationResults(validatorResult);
        deserializedRawTim.setFailedMessage("Invalid TIM message");

        KeyValue<RsuTimKey, ProcessedTim> result = timTransformer.transform(null, deserializedRawTim);

        // Verify failure handling
        assertNotNull(result);
        assertNotNull(result.key);
        assertEquals("ERROR", result.key.getRsuId());
        assertNotNull(result.value);

        // Verify failure processing
        ProcessedTim processedTim = result.value;
        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(!processedTim.getCompliance().get(0).isCompliant());
    }

    @Test
    public void testTransformWithNullInput() {
        // Test error handling with null input
        KeyValue<RsuTimKey, ProcessedTim> result = timTransformer.transform(null, null);

        // Verify error handling
        assertNotNull(result);
        assertNotNull(result.key);
        assertEquals("ERROR", result.key.getRsuId());
        assertNull(result.value);
    }

    @Test
    public void testTransformWithException() {
        // Test exception handling by providing malformed data
        DeserializedRawMessageFrame deserializedRawTim = new DeserializedRawMessageFrame();
        deserializedRawTim.setOdeMessageFrameData(null); // This should cause an exception
        deserializedRawTim.setValidationFailure(false);
        deserializedRawTim.setValidationResults(new JsonValidatorResult());

        KeyValue<RsuTimKey, ProcessedTim> result = timTransformer.transform(null, deserializedRawTim);

        // Verify exception handling
        assertNotNull(result);
        assertNotNull(result.key);
        assertEquals("ERROR", result.key.getRsuId());
        assertNull(result.value);
    }
}
