package us.dot.its.jpo.geojsonconverter.converter.map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.networknt.schema.Error;
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

}
