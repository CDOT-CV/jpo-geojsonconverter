package us.dot.its.jpo.geojsonconverter.converter.map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.GeometryOutputMode;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;
import us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.validator.MapJsonValidator;

@SpringBootTest(properties = "spring.kafka.streams.auto-startup=false")
@ActiveProfiles("test")
public class MapTopologyTest {
    String kafkaTopicOdeMapJson = "topic.OdeMapJson";
    String kafkaTopicMapGeoJson = "topic.ProcessedMap";
    String kafkaTopicMapWKT = "topic.ProcessedMapWKT";
    String odeMapJsonString;

    @Autowired
    private MapJsonValidator mapJsonValidator;

    @BeforeEach
    void setup() throws IOException {
        odeMapJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/valid.map.json")));
    }

    @Test
    public void testTopologyGeoJson() {
        Topology topology = MapTopology.build(kafkaTopicOdeMapJson, kafkaTopicMapGeoJson, kafkaTopicMapWKT,
                mapJsonValidator, GeometryOutputMode.GEOJSON_ONLY);
        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<Void, String> inputTopic = driver.createInputTopic(kafkaTopicOdeMapJson,
                    Serdes.Void().serializer(), Serdes.String().serializer());
            TestOutputTopic<RsuIntersectionKey, ProcessedMap<LineString>> outputTopic =
                    driver.createOutputTopic(kafkaTopicMapGeoJson, JsonSerdes.RsuIntersectionKey().deserializer(),
                            JsonSerdes.ProcessedMapGeoJson().deserializer());

            // Send serialized OdeMapJson to OdeMapJson topic
            inputTopic.pipeInput(odeMapJsonString);

            // Check MapGeoJson topic for properly converted message data
            List<KeyValue<RsuIntersectionKey, ProcessedMap<LineString>>> mapGeoJsonResults =
                    outputTopic.readKeyValuesToList();
            assertEquals(1, mapGeoJsonResults.size());

            KeyValue<RsuIntersectionKey, ProcessedMap<LineString>> mapGeoJson = mapGeoJsonResults.get(0);
            assertNotNull(mapGeoJson.key);
            assertEquals("172.18.0.1", mapGeoJson.key.getRsuId());
            assertEquals(12112, mapGeoJson.key.getIntersectionId());
            assertNotNull(mapGeoJson.value);
            assertEquals(27, mapGeoJson.value.getMapFeatureCollection().getFeatures().length);
            assertEquals(2,
                    mapGeoJson.value.getMapFeatureCollection().getFeatures()[0].getProperties().getIngressApproach());
            assertEquals(false, mapGeoJson.value.getProperties().getCti4501Conformant());
            assertEquals(4, mapGeoJson.value.getProperties().getValidationMessages().size());
        }
    }

    @Test
    public void testTopologyWKT() {
        Topology topology = MapTopology.build(kafkaTopicOdeMapJson, kafkaTopicMapGeoJson, kafkaTopicMapWKT,
                mapJsonValidator, GeometryOutputMode.WKT);
        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<Void, String> inputOdeTopic = driver.createInputTopic(kafkaTopicOdeMapJson,
                    Serdes.Void().serializer(), Serdes.String().serializer());
            TestOutputTopic<RsuIntersectionKey, ProcessedMap<String>> outputTopic =
                    driver.createOutputTopic(kafkaTopicMapWKT, JsonSerdes.RsuIntersectionKey().deserializer(),
                            JsonSerdes.ProcessedMapWKT().deserializer());

            // Send serialized OdeMapJson to OdeMapJson topic
            inputOdeTopic.pipeInput(odeMapJsonString);

            // Check MapWKT topic for properly converted message data
            List<KeyValue<RsuIntersectionKey, ProcessedMap<String>>> mapWKTResults = outputTopic.readKeyValuesToList();
            assertEquals(1, mapWKTResults.size());

            KeyValue<RsuIntersectionKey, ProcessedMap<String>> mapWKT = mapWKTResults.get(0);
            assertNotNull(mapWKT.key);
            assertEquals("172.18.0.1", mapWKT.key.getRsuId());
            assertEquals(12112, mapWKT.key.getIntersectionId());
            assertNotNull(mapWKT.value);
            assertEquals(27, mapWKT.value.getMapFeatureCollection().getFeatures().length);
            assertEquals(
                    "LINESTRING (-105.0873158756377 39.580832437103176, -105.08774305288308 39.58155028234169, -105.08788214783118 39.58177536344629, -105.0880953890977 39.58214905744837, -105.08859742005322 39.58299092652536, -105.08910539363902 39.58384792695694, -105.08960266448658 39.5847078094986)",
                    mapWKT.value.getMapFeatureCollection().getFeatures()[0].getGeometry());
        }
    }

    @Test
    public void testTopologyFailureGeoJson() {
        Topology topology = MapTopology.build(kafkaTopicOdeMapJson, kafkaTopicMapGeoJson, kafkaTopicMapWKT,
                mapJsonValidator, GeometryOutputMode.GEOJSON_ONLY);
        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            TestInputTopic<Void, String> inputTopic = driver.createInputTopic(kafkaTopicOdeMapJson,
                    Serdes.Void().serializer(), Serdes.String().serializer());
            TestOutputTopic<RsuIntersectionKey, ProcessedMap<LineString>> outputTopic =
                    driver.createOutputTopic(kafkaTopicMapGeoJson, JsonSerdes.RsuIntersectionKey().deserializer(),
                            JsonSerdes.ProcessedMapGeoJson().deserializer());

            // Send serialized OdeMapJson to OdeMapJson topic
            inputTopic.pipeInput("{");

            // Check MapGeoJson topic for properly converted message data
            List<KeyValue<RsuIntersectionKey, ProcessedMap<LineString>>> mapGeoJsonResults =
                    outputTopic.readKeyValuesToList();
            assertEquals(1, mapGeoJsonResults.size());

            KeyValue<RsuIntersectionKey, ProcessedMap<LineString>> mapGeoJson = mapGeoJsonResults.get(0);
            assertNotNull(mapGeoJson.key);
            assertEquals("ERROR", mapGeoJson.key.getRsuId());
            assertNotNull(mapGeoJson.value);
            assertEquals(1, mapGeoJson.value.getProperties().getValidationMessages().size());
        }
    }
}
