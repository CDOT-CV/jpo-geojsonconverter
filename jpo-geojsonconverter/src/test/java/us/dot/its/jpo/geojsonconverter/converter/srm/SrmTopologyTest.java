package us.dot.its.jpo.geojsonconverter.converter.srm;

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
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuVehicleIdKey;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.srm.ProcessedSrm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.srm.SrmProperties;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.geojsonconverter.validator.SrmJsonValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@Slf4j
public class SrmTopologyTest {
    final String inputTopicName = "topic.OdeSrmJson";
    final String outputTopicName = "topic.ProcessedSrm";

    @ParameterizedTest
    @MethodSource("params")
    public void topologyTest(String inputJson, int expectNumberOfRequests, boolean expectValid) {
        SrmJsonValidator validator = new SrmJsonValidator("classpath:schemas/srm.schema.json");
        SrmConverter converter = new SrmConverter();
        Topology topology = SrmTopology.build(inputTopicName, outputTopicName, validator, converter);
        try (var driver = new TopologyTestDriver(topology)) {
            var inputTopic = driver.createInputTopic(inputTopicName, new VoidSerializer(), new StringSerializer());
            var outputTopic = driver.createOutputTopic(outputTopicName,
                    new JsonDeserializer<>(RsuVehicleIdKey.class), new JsonDeserializer<>(ProcessedSrm.class));

            inputTopic.pipeInput(inputJson);

            List<KeyValue<RsuVehicleIdKey, ProcessedSrm>> results = outputTopic.readKeyValuesToList();

            assertEquals(1, results.size());

            KeyValue<RsuVehicleIdKey, ProcessedSrm> result = results.getFirst();
            RsuVehicleIdKey key = result.key;
            assertNotNull(key);
            assertEquals("172.18.0.1", key.getRsuId());
            ProcessedSrm processedSrm = result.value;
            assertNotNull(processedSrm);
            SrmProperties properties = processedSrm.getProperties();
            assertNotNull(properties);
            assertNotNull(properties.getAsn1());
            assertNotNull(properties.getOdeReceivedAt());
            assertEquals("SRM", properties.getMessageType());
            assertThat(properties.getRequests(), hasSize(equalTo(expectNumberOfRequests)));
            if (expectValid) {
                assertThat("expected valid message but has validation messages",
                        properties.getValidationMessages(), hasSize(equalTo(0)));
            } else {
                assertThat("expected invalid message but has no validation messages",
                        properties.getValidationMessages(), hasSize(greaterThan(0)));
            }


            Point geometry = processedSrm.getGeometry();
            assertNotNull(geometry);
        }
    }

    public static Stream<Arguments> params() throws IOException {
        return Stream.of(
                Arguments.of( loadResource("json/valid.srm.json"), 1, true ),
                Arguments.of( loadResource("json/valid.srm-multi.json"), 2, true ),
                Arguments.of( loadResource("json/invalid.srm.json"), 1, false)
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
