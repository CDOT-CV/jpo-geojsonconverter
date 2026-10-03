package us.dot.its.jpo.geojsonconverter.converter.spat;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.networknt.schema.Error;

import us.dot.its.jpo.asn.j2735.r2024.SPAT.SPATMessageFrame;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.TimeMark;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.spat.DeserializedRawSpat;
import us.dot.its.jpo.geojsonconverter.pojos.spat.ProcessedSpat;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.geojsonconverter.utils.ProcessedSchemaVersions;
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

    @Test
    public void testApplyValidSpat() {
        DeserializedRawSpat rawSpat = new DeserializedRawSpat();
        rawSpat.setOdeSpatMessageFrameData(spatMF);
        rawSpat.setValidatorResults(new JsonValidatorResult());

        KeyValue<RsuIntersectionKey, ProcessedSpat> result = spatProcessedJsonConverter.apply(null, rawSpat);

        assertEquals("172.18.0.1", result.key.getRsuId());
        assertEquals(8804, result.key.getIntersectionId());
        assertNotNull(result.value);
        assertEquals("172.18.0.1", result.value.getOriginIp());
        assertEquals(8804, result.value.getIntersectionId());
        assertEquals(0, result.value.getRevision());
        assertNotNull(result.value.getUtcTimeStampTS());
        assertEquals(8, result.value.getStates().size());
        assertEquals(1, result.value.getStates().get(0).getStateTimeSpeed().size());
        assertNotNull(result.value.getStates().get(0).getStateTimeSpeed().get(0).getTiming().getMinEndTime());
        assertNull(result.value.getStates().get(0).getStateTimeSpeed().get(0).getSpeeds());
    }

    @Test
    void testCurrentAndPredictedPhaseStartsUseTheirOwnEarliestEnds() {
        var spat = ((SPATMessageFrame) spatMF.getPayload().getData()).getValue();
        var intersection = spat.getIntersections().getFirst();
        spat.setTimeStamp(null);
        intersection.setMoy(null);
        intersection.setTimeStamp(null);
        spatMF.getMetadata().setOdeReceivedAt("2024-06-15T15:30:00Z");

        var currentEvent = intersection.getStates().getFirst().getState_time_speed().getFirst();
        var currentTiming = currentEvent.getTiming();
        currentTiming.setStartTime(new TimeMark(17400)); // Current phase began at 15:29.
        currentTiming.setMinEndTime(new TimeMark(18100)); // Earliest end is 15:30:10.
        currentTiming.setMaxEndTime(new TimeMark(18200));
        currentTiming.setLikelyTime(new TimeMark(18150));
        currentTiming.setNextTime(new TimeMark(600));

        var predictedEvent = intersection.getStates().get(1).getState_time_speed().getFirst();
        predictedEvent.getTiming().setStartTime(new TimeMark(600)); // Predicted phase starts at 16:01.
        predictedEvent.getTiming().setMinEndTime(new TimeMark(1200));
        intersection.getStates().getFirst().getState_time_speed().add(predictedEvent);

        DeserializedRawSpat rawSpat = new DeserializedRawSpat();
        rawSpat.setOdeSpatMessageFrameData(spatMF);
        rawSpat.setValidatorResults(new JsonValidatorResult());
        var result = spatProcessedJsonConverter.apply(null, rawSpat).value;

        assertNotNull(result);
        assertEquals(ProcessedSchemaVersions.PROCESSED_SPAT_SCHEMA_VERSION, result.getSchemaVersion());
        assertEquals(Instant.parse("2024-06-15T15:30:00Z"), result.getUtcTimeStampTS());
        var events = result.getStates().getFirst().getStateTimeSpeed();
        assertEquals(2, events.size());
        var current = events.getFirst().getTiming();
        assertEquals(ZonedDateTime.parse("2024-06-15T15:29:00Z"), current.getStartTime());
        assertEquals(ZonedDateTime.parse("2024-06-15T15:30:10Z"), current.getMinEndTime());
        assertEquals(ZonedDateTime.parse("2024-06-15T15:30:20Z"), current.getMaxEndTime());
        assertEquals(ZonedDateTime.parse("2024-06-15T15:30:15Z"), current.getLikelyTime());
        assertEquals(ZonedDateTime.parse("2024-06-15T16:01:00Z"), current.getNextTime());
        var predicted = events.get(1).getTiming();
        assertEquals(ZonedDateTime.parse("2024-06-15T16:01:00Z"), predicted.getStartTime());
        assertEquals(ZonedDateTime.parse("2024-06-15T16:02:00Z"), predicted.getMinEndTime());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {36050, 36111})
    void testUnavailableEarliestEndRetainsStartFallback(Long endMark) {
        var spat = ((SPATMessageFrame) spatMF.getPayload().getData()).getValue();
        var intersection = spat.getIntersections().getFirst();
        spat.setTimeStamp(null);
        intersection.setMoy(null);
        intersection.setTimeStamp(null);
        spatMF.getMetadata().setOdeReceivedAt("2024-06-15T15:30:00Z");
        var timing = intersection.getStates().getFirst().getState_time_speed().getFirst().getTiming();
        timing.setStartTime(new TimeMark(17400));
        timing.setMinEndTime(endMark == null ? null : new TimeMark(endMark));

        var result = spatProcessedJsonConverter.createProcessedSpat(spat, spatMF.getMetadata(),
                new JsonValidatorResult());
        var processedTiming = result.getStates().getFirst().getStateTimeSpeed().getFirst().getTiming();

        assertEquals(ZonedDateTime.parse("2024-06-15T16:29:00Z"), processedTiming.getStartTime());
        if (endMark == null) {
            assertNull(processedTiming.getMinEndTime());
        } else {
            assertEquals(Instant.EPOCH, processedTiming.getMinEndTime().toInstant());
        }
    }

}
