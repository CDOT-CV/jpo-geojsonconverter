package us.dot.its.jpo.geojsonconverter.converter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.IntStream;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import us.dot.its.jpo.geojsonconverter.GeoJsonConverterProperties;
import us.dot.its.jpo.geojsonconverter.converter.rtcm.RTCMConverter;
import us.dot.its.jpo.geojsonconverter.converter.srm.SrmConverter;
import us.dot.its.jpo.geojsonconverter.converter.ssm.SsmConverter;
import us.dot.its.jpo.geojsonconverter.converter.tim.TimConverter;
import us.dot.its.jpo.geojsonconverter.validator.*;

class JsonConverterStartupTest {
    private static final List<String> APPLICATION_IDS = List.of("processedmapjson", "processedspatjson",
            "processedbsmjson", "processedpsmjson", "processedrcmjson", "processedtimjson",
            "processedsrmjson", "processedssmjson");

    @Test
    void allEightTopologiesStartInExistingOrder() throws InterruptedException {
        var streams = createStreams();
        var hooks = new ArrayList<Thread>();
        var startedIds = new ArrayList<String>();
        startController(streams, hooks, startedIds);

        assertEquals(APPLICATION_IDS, startedIds);
        assertEquals(8, hooks.size());
        for (int index = 0; index < streams.size(); index++) {
            verify(streams.get(index)).start();
            verify(streams.get(index), never()).close();
            if (index < 6) {
                verify(streams.get(index)).setUncaughtExceptionHandler(any(StreamsUncaughtExceptionHandler.class));
            } else {
                verify(streams.get(index), never())
                        .setUncaughtExceptionHandler(any(StreamsUncaughtExceptionHandler.class));
            }
        }
        for (var hook : hooks) {
            hook.start();
            hook.join(5000);
            assertFalse(hook.isAlive());
        }
        streams.forEach(stream -> verify(stream).close());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    void startupFailureKeepsEarlierStreamsRunningAndSkipsLaterTopologies(int failingPosition) {
        var streams = createStreams();
        var hooks = new ArrayList<Thread>();
        var startedIds = new ArrayList<String>();
        doThrow(new IllegalStateException("start failed")).when(streams.get(failingPosition - 1)).start();

        assertDoesNotThrow(() -> startController(streams, hooks, startedIds));

        assertEquals(APPLICATION_IDS.subList(0, failingPosition), startedIds);
        assertEquals(failingPosition - 1, hooks.size(), "Failed stream's hook should be removed");
        for (int index = 0; index < failingPosition - 1; index++) {
            verify(streams.get(index)).start();
            verify(streams.get(index), never()).close();
        }
        verify(streams.get(failingPosition - 1)).close();
        for (int index = failingPosition; index < streams.size(); index++) {
            verifyNoInteractions(streams.get(index));
        }
    }

    private static List<KafkaStreams> createStreams() {
        return IntStream.range(0, 8).mapToObj(index -> mock(KafkaStreams.class)).toList();
    }

    private static void startController(List<KafkaStreams> streams, List<Thread> hooks, List<String> startedIds) {
        var delegate = new KafkaStreamsStarter((topology, properties) -> {
            int index = startedIds.size();
            startedIds.add(properties.getProperty(StreamsConfig.APPLICATION_ID_CONFIG));
            return streams.get(index);
        }, hooks::add, hooks::remove);

        // Delegate the controller's starter to broker-free streams and in-memory hook registration.
        try (var construction = mockConstruction(KafkaStreamsStarter.class, (starter, context) -> {
            doAnswer(invocation -> {
                delegate.start(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2));
                return null;
            }).when(starter).start(any(Topology.class), any(Properties.class), any(StreamsUncaughtExceptionHandler.class));
            doAnswer(invocation -> {
                delegate.start(invocation.getArgument(0), invocation.getArgument(1));
                return null;
            }).when(starter).start(any(Topology.class), any(Properties.class));
        })) {
            new JsonConverterServiceController(properties(), mock(MapJsonValidator.class), mock(SpatJsonValidator.class),
                    mock(BsmJsonValidator.class), mock(PsmJsonValidator.class), mock(RTCMJsonValidator.class),
                    mock(RTCMConverter.class), mock(TimJsonValidator.class), mock(TimConverter.class),
                    mock(SrmJsonValidator.class), mock(SrmConverter.class), mock(SsmJsonValidator.class),
                    mock(SsmConverter.class));
            assertEquals(1, construction.constructed().size());
        }
    }

    private static GeoJsonConverterProperties properties() {
        var properties = new GeoJsonConverterProperties();
        properties.setKafkaBrokers("localhost:9092");
        properties.setStreamsConfigAcks("all");
        properties.setKafkaTopicOdeRtcmJson("topic.OdeRtcmJson");
        properties.setKafkaTopicProcessedRtcm("topic.ProcessedRtcm");
        properties.setKafkaTopicOdeTimJson("topic.OdeTimJson");
        properties.setKafkaTopicProcessedTim("topic.ProcessedTim");
        properties.setKafkaTopicOdeSrmJson("topic.OdeSrmJson");
        properties.setKafkaTopicProcessedSrm("topic.ProcessedSrm");
        properties.setKafkaTopicOdeSsmJson("topic.OdeSsmJson");
        properties.setKafkaTopicProcessedSsm("topic.ProcessedSsm");
        return properties;
    }
}
