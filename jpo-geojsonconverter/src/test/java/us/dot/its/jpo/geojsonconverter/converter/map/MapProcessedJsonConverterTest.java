package us.dot.its.jpo.geojsonconverter.converter.map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.networknt.schema.Error;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;
import us.dot.its.jpo.asn.j2735.r2024.MapData.MapDataMessageFrame;
import us.dot.its.jpo.asn.j2735.r2024.Common.Latitude;
import us.dot.its.jpo.asn.j2735.r2024.Common.Longitude;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeOffsetPointXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeSetXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeXY;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_XY_20b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_LLmD_64b;
import us.dot.its.jpo.asn.j2735.r2024.Common.Offset_B10;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.DeserializedRawMap;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

@Slf4j
public class MapProcessedJsonConverterTest {
    MapProcessedJsonConverter mapProcessedJsonConverter;
    OdeMessageFrameData mapMF;
    DeserializedRawMap rawMap;

    @BeforeEach
    void setup() throws IOException {
        String odeMapJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/valid.map.json")));

        try (JsonDeserializer<OdeMessageFrameData> odeMapDeserializer =
                new JsonDeserializer<>(OdeMessageFrameData.class)) {
            mapMF = odeMapDeserializer.deserialize("test-topic", odeMapJsonString.getBytes());
        }

        JsonValidatorResult validatorResults = new JsonValidatorResult();
        Exception exception = new Exception("test_exception");
        validatorResults.addException(exception);
        List<Error> validationMessages = new ArrayList<>();
        validatorResults.addValidationMessages(validationMessages);

        rawMap = new DeserializedRawMap();
        rawMap.setOdeMapMessageFrameData(mapMF);
        rawMap.setValidatorResults(validatorResults);
        mapProcessedJsonConverter = new MapProcessedJsonConverter();
    }

    @Test
    public void testConstructor() {
        assertNotNull(mapProcessedJsonConverter);
    }


    @Test
    public void testApply() {
        KeyValue<RsuIntersectionKey, ProcessedMap<LineString>> mapFeatureCollection =
                mapProcessedJsonConverter.apply(null, rawMap);
        log.info("mapFeatureCollection: {}", mapFeatureCollection);
        assertNotNull(mapFeatureCollection.key);
        assertEquals("172.18.0.1", mapFeatureCollection.key.getRsuId());
        assertEquals(12112, mapFeatureCollection.key.getIntersectionId());
        assertNotNull(mapFeatureCollection.value);
        assertEquals(27, mapFeatureCollection.value.getMapFeatureCollection().getFeatures().length);
        var connectingLaneFeatures = mapFeatureCollection.value.getConnectingLanesFeatureCollection().getFeatures();
        assertTrue(connectingLaneFeatures.length > 0);
        assertEquals("18-6", connectingLaneFeatures[0].getId());
        assertEquals(6, connectingLaneFeatures[0].getProperties().getSignalGroupId());
    }

    @Test
    void mapTimestampUsesMinutePrecisionIndependentOfReceivedSeconds() {
        var first = convertMapTimestamp(1000L, "2026-01-01T00:00:12.345Z");
        var second = convertMapTimestamp(1000L, "2026-01-01T00:00:56.789Z");

        assertEquals(Instant.parse("2026-01-01T16:40:00Z"), first);
        assertEquals(first, second);
    }

    @Test
    void absentOrUnavailableMapTimestampFallsBackToFullReceivedTimestamp() {
        assertEquals(Instant.parse("2026-01-01T00:00:12.345Z"),
                convertMapTimestamp(null, "2026-01-01T00:00:12.345Z"));
        assertEquals(Instant.parse("2026-01-01T00:00:56.789Z"),
                convertMapTimestamp(527040L, "2026-01-01T00:00:56.789Z"));
    }

    @Test
    void mapTimestampInfersPreviousYearForLastMinuteReceivedOnNewYearsDay() {
        assertEquals(Instant.parse("2025-12-31T23:59:00Z"),
                convertMapTimestamp(525599L, "2026-01-01T00:00:01.000Z"));
    }

    private Instant convertMapTimestamp(Long minuteOfYear, String receivedAt) {
        mapMF.getMetadata().setOdeReceivedAt(receivedAt);
        MapDataMessageFrame messageFrame = (MapDataMessageFrame) mapMF.getPayload().getData();
        messageFrame.getValue().setTimeStamp(
                minuteOfYear != null ? new MinuteOfTheYear(minuteOfYear) : null);

        KeyValue<RsuIntersectionKey, ProcessedMap<LineString>> converted =
                mapProcessedJsonConverter.apply(null, rawMap);
        assertNotNull(converted.value);
        return converted.value.getProperties().getTimeStamp().toInstant();
    }

    @Test
    public void testApplyValidationFailure() {
        rawMap.setValidationFailure(true);
        rawMap.setFailedMessage("Failed to transform");
        KeyValue<RsuIntersectionKey, ProcessedMap<LineString>> mapFeatureCollection =
                mapProcessedJsonConverter.apply(null, rawMap);
        assertNotNull(mapFeatureCollection.key);
        assertEquals("ERROR", mapFeatureCollection.key.getRsuId());
        assertNotNull(mapFeatureCollection.value);
        assertEquals(1, mapFeatureCollection.value.getProperties().getValidationMessages().size());
    }

    @Test
    public void testApplyException() {
        KeyValue<RsuIntersectionKey, ProcessedMap<LineString>> mapFeatureCollection =
                mapProcessedJsonConverter.apply(null, null);
        assertNotNull(mapFeatureCollection.key);
        assertEquals("ERROR", mapFeatureCollection.key.getRsuId());
        assertNull(mapFeatureCollection.value);
    }

    @Test
    void nodeConversionListConvertsRelativeAndAbsoluteOffsetsAndSkipsEmptyNodes() {
        NodeSetXY nodeSet = new NodeSetXY();

        Node_XY_20b relativeOffsets = new Node_XY_20b();
        relativeOffsets.setX(new Offset_B10(15L));
        relativeOffsets.setY(new Offset_B10(-25L));
        NodeOffsetPointXY relativeDelta = new NodeOffsetPointXY();
        relativeDelta.setNode_XY1(relativeOffsets);
        NodeXY relativeNode = new NodeXY();
        relativeNode.setDelta(relativeDelta);
        nodeSet.add(relativeNode);

        Node_LLmD_64b absolutePosition = new Node_LLmD_64b();
        absolutePosition.setLon(new Longitude(-1040000000));
        absolutePosition.setLat(new Latitude(410000000));
        NodeOffsetPointXY absoluteDelta = new NodeOffsetPointXY();
        absoluteDelta.setNode_LatLon(absolutePosition);
        NodeXY absoluteNode = new NodeXY();
        absoluteNode.setDelta(absoluteDelta);
        nodeSet.add(absoluteNode);

        NodeXY emptyNode = new NodeXY();
        emptyNode.setDelta(new NodeOffsetPointXY());
        nodeSet.add(emptyNode);

        var converted = mapProcessedJsonConverter.nodeConversionList(nodeSet);

        assertEquals(2, converted.size());
        assertArrayEquals(new Integer[] {15, -25}, converted.get(0).getDelta());
        assertArrayEquals(new Integer[] {-104, 41}, converted.get(1).getDelta());
    }

}
