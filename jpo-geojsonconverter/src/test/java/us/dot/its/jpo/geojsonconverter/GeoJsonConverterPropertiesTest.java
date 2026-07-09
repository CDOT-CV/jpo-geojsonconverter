package us.dot.its.jpo.geojsonconverter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;



public class GeoJsonConverterPropertiesTest {

    
    GeoJsonConverterProperties testGeoJsonConverterProperties;
    BuildProperties mockBuildProperties;

    @BeforeEach
    void setup() {
        testGeoJsonConverterProperties = new GeoJsonConverterProperties();
        testGeoJsonConverterProperties.initialize();
        testGeoJsonConverterProperties.setStreamsConfigReplicationFactor(1);
        testGeoJsonConverterProperties.setStreamsConfigAcks("1");
        testGeoJsonConverterProperties.setStreamsConfigNumStreamThreads(10);
        testGeoJsonConverterProperties.setStreamsConfigCacheMaxBytesBuffering(1048576);
        testGeoJsonConverterProperties.setStreamsConfigCommitIntervalMs(100);
    }

    @Test
    public void testInit() {
        try {
                new GeoJsonConverterProperties();
        } catch (Exception e) {
                fail("Unexpected exception: " + e);
        }
    }

    @Test
    public void testInitShouldCatchUnknownHostException() {
        String expectedBroker = "localhost:9092";
        assertEquals(expectedBroker, testGeoJsonConverterProperties.getKafkaBrokers(), "Incorrect KafkaBrokers");
    }

    @Test
    public void testKafkaBrokersSetterAndGetter() {
        String testKafkaBrokers = "testKafkaBrokers";
        testGeoJsonConverterProperties.setKafkaBrokers(testKafkaBrokers);
        assertEquals(testKafkaBrokers, testGeoJsonConverterProperties.getKafkaBrokers(), "Incorrect KafkaBrokers");
    }

    @Test
    public void testEnvSetterAndGetter() {
        testGeoJsonConverterProperties.setEnv(null);
        assertNull(testGeoJsonConverterProperties.getEnv());
    }

    @Test
    public void testKafkaTopicOdeSpatJsonSetterAndGetter() {
        String testKafkaTopicOdeSpatJson = "testKafkaTopicOdeSpatJson";
        testGeoJsonConverterProperties.setKafkaTopicOdeSpatJson(testKafkaTopicOdeSpatJson);
        assertEquals(testKafkaTopicOdeSpatJson, testGeoJsonConverterProperties.getKafkaTopicOdeSpatJson(), "Incorrect KafkaTopicOdeSpatJson");
    }

    @Test
    public void testKafkaTopicSpatGeoJsonSetterAndGetter() {
        String testKafkaTopicSpatGeoJson = "testKafkaTopicSpatGeoJson";
        testGeoJsonConverterProperties.setKafkaTopicSpatGeoJson(testKafkaTopicSpatGeoJson);
        assertEquals(testKafkaTopicSpatGeoJson, testGeoJsonConverterProperties.getKafkaTopicSpatGeoJson(), "Incorrect KafkaTopicSpatGeoJson");
    }

    @Test
    public void testKafkaTopicOdeMapJsonSetterAndGetter() {
        String testKafkaTopicOdeMapJson = "testKafkaTopicOdeMapJson";
        testGeoJsonConverterProperties.setKafkaTopicOdeMapJson(testKafkaTopicOdeMapJson);
        assertEquals(testKafkaTopicOdeMapJson, testGeoJsonConverterProperties.getKafkaTopicOdeMapJson(), "Incorrect KafkaTopicOdeMapJson");
    }

    @Test
    public void testKafkaTopicMapGeoJsonSetterAndGetter() {
        String testKafkaTopicMapGeoJson = "testKafkaTopicMapGeoJson";
        testGeoJsonConverterProperties.setKafkaTopicProcessedMap(testKafkaTopicMapGeoJson);
        assertEquals(testKafkaTopicMapGeoJson, testGeoJsonConverterProperties.getKafkaTopicProcessedMap(), "Incorrect KafkaTopicMapGeoJson");
    }

    @Test
    public void testStreamProperties() {
        Properties streamProps = testGeoJsonConverterProperties.createStreamProperties("test-props");
        assertNotNull(streamProps);
    }
}
