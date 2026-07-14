package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.VoidSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuStationIdKey;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.ProcessedRTCM;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.rtcm.RTCMProperties;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.geojsonconverter.validator.RTCMJsonValidator;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class RTCMTopologyTest {

    final String inputTopicName = "topic.OdeRtcmJson";
    final String outputTopicName = "topic.ProcessedRtcm";

    @ParameterizedTest
    @MethodSource("params")
    public void topologyTest(String inputJson, boolean expectCti4501Conformant, int expectStationId, boolean expectRev3) {
        RTCMJsonValidator validator = new RTCMJsonValidator("classpath:schemas/rtcm.schema.json");
        RTCMDecoder decoder = new RTCMDecoder(false);
        RTCMConverter converter = new RTCMConverter(decoder);
        Topology topology = RTCMTopology.build(inputTopicName, outputTopicName, validator, converter);
        try (var driver = new TopologyTestDriver(topology)) {

            var inputTopic = driver.createInputTopic(inputTopicName,
                    new VoidSerializer(), new StringSerializer());
            var outputTopic = driver.createOutputTopic(outputTopicName,
                    new JsonDeserializer<>(RsuStationIdKey.class), new JsonDeserializer<>(ProcessedRTCM.class));


            inputTopic.pipeInput(inputJson);

            List<KeyValue<RsuStationIdKey, ProcessedRTCM>> results = outputTopic.readKeyValuesToList();

            assertEquals(1, results.size());

            KeyValue<RsuStationIdKey, ProcessedRTCM> result = results.getFirst();
            assertNotNull(result.key);
            RsuStationIdKey key = result.key;
            assertEquals("172.18.0.1", key.getRsuId());

            assertNotNull(result.value);
            ProcessedRTCM value = result.value;
            RTCMProperties properties = value.getProperties();
            assertNotNull(properties);
            assertEquals("172.18.0.1", properties.getOriginIp());

            assertNotNull(properties.getAsn1());
            assertNotNull(properties.getOdeReceivedAt());

            var geometry = value.getGeometry();
            assertNotNull(geometry);
            assertEquals("Point", geometry.getType());

            assertNotNull(properties.getRev());

            if (expectRev3) {
                assertEquals("rtcmRev3", properties.getRev());
                assertNotNull(key.getStationId());
                assertEquals(expectStationId, key.getStationId().intValue());
                assertNotNull(properties.getStationId());
                assertEquals(expectStationId, properties.getStationId().intValue());
                assertNotNull(properties.getMessageTypes());
                assertFalse(properties.getMessageTypes().isEmpty());
            } else {
                // Partial decode can't get the station id or message types if not rev3
                assertEquals("rtcmRev2", properties.getRev());
            }


            assertEquals(expectCti4501Conformant, properties.isCti4501Conformant());
        }
    }

    public static Stream<Arguments> params() throws IOException {
        return Stream.of(
                Arguments.of( loadResource("json/valid.rtcm.json"), true, 2432, true ),
                Arguments.of( loadResource("json/invalid.rtcm.json"), false, 2432, true ),
                Arguments.of( loadResource("json/invalid2.rtcm.json"), false, 48, false )
        );
    }

    private static String loadResource(String path) throws IOException {
        Resource resource = getResource(path);
        return readResource(resource);
    }

    private static Resource getResource(String path) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        return resourceLoader.getResource("classpath:" + path);
    }

    private static String readResource(Resource resource) throws IOException {
        byte[] bytes = resource.getInputStream().readAllBytes();
        return  new String(bytes, StandardCharsets.UTF_8);
    }

}
