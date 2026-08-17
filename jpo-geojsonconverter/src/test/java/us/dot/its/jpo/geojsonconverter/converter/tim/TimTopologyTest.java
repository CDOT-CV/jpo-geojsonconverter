package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.VoidSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.junit.jupiter.api.Test;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.geojsonconverter.validator.TimJsonValidator;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.geojsonconverter.pojos.common.Ieee1609Dot2SignedDataMetadata;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;

class TimTopologyTest {

    private static final String INPUT_TOPIC = "topic.OdeTimJson";
    private static final String OUTPUT_TOPIC = "topic.ProcessedTim";

    @Test
    void convertsValidatedRawTimToProcessedTim() throws IOException {
        Topology topology = TimTopology.build(INPUT_TOPIC, OUTPUT_TOPIC,
                new TimJsonValidator("classpath:schemas/tim.schema.json"), new TimConverter(new TimGeometryConverter()));

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            var inputTopic = driver.createInputTopic(INPUT_TOPIC, new VoidSerializer(), new ByteArraySerializer());
            var outputTopic = driver.createOutputTopic(OUTPUT_TOPIC, new JsonDeserializer<>(RsuTimKey.class),
                    new JsonDeserializer<>(ProcessedTim.class));

            inputTopic.pipeInput(loadResource("/json/sample.ode-tim.json"));

            List<KeyValue<RsuTimKey, ProcessedTim>> results = outputTopic.readKeyValuesToList();
            assertEquals(1, results.size());

            KeyValue<RsuTimKey, ProcessedTim> result = results.getFirst();
            assertEquals("172.27.0.1", result.key.getRsuId());
            assertEquals("18D4A500000D7BA133", result.key.getPacketId());
            assertEquals(1, result.key.getMsgCnt());
            assertNotNull(result.value);
            assertEquals("TIM", result.value.getMessageType());
            assertTrue(result.value.getCompliance().getFirst().getValidationMessages().isEmpty());
        }
    }

    @Test
    void convertsMalformedRawTimToFailureResult() {
        Topology topology = TimTopology.build(INPUT_TOPIC, OUTPUT_TOPIC,
                new TimJsonValidator("classpath:schemas/tim.schema.json"), new TimConverter(new TimGeometryConverter()));

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            var inputTopic = driver.createInputTopic(INPUT_TOPIC, new VoidSerializer(), new ByteArraySerializer());
            var outputTopic = driver.createOutputTopic(OUTPUT_TOPIC, new JsonDeserializer<>(RsuTimKey.class),
                    new JsonDeserializer<>(ProcessedTim.class));

            inputTopic.pipeInput("not valid JSON".getBytes(StandardCharsets.UTF_8));

            List<KeyValue<RsuTimKey, ProcessedTim>> results = outputTopic.readKeyValuesToList();
            assertEquals(1, results.size());
            assertEquals("ERROR", results.getFirst().key.getRsuId());
            assertTrue(!results.getFirst().value.getCompliance().getFirst().isCompliant());
        }
    }


    @Test
    void dropsResultWhenTimConversionThrows() throws IOException {
        TimConverter failingConverter = new TimConverter(new TimGeometryConverter()) {
            @Override
            public ProcessedTim createProcessedTim(TravelerInformation travelerInfo, OdeMessageFrameMetadata metadata,
                    Ieee1609Dot2SignedDataMetadata signedDataMetadata) {
                throw new IllegalStateException("conversion failure");
            }
        };
        Topology topology = TimTopology.build(INPUT_TOPIC, OUTPUT_TOPIC,
                new TimJsonValidator("classpath:schemas/tim.schema.json"), failingConverter);

        try (TopologyTestDriver driver = new TopologyTestDriver(topology)) {
            var inputTopic = driver.createInputTopic(INPUT_TOPIC, new VoidSerializer(), new ByteArraySerializer());
            var outputTopic = driver.createOutputTopic(OUTPUT_TOPIC, new JsonDeserializer<>(RsuTimKey.class),
                    new JsonDeserializer<>(ProcessedTim.class));

            inputTopic.pipeInput(loadResource("/json/sample.ode-tim.json"));

            assertTrue(outputTopic.isEmpty());
        }
    }

    private byte[] loadResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: " + resourcePath);
            }
            return inputStream.readAllBytes();
        }
    }
}
