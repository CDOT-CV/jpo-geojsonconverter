package us.dot.its.jpo.geojsonconverter.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;

import us.dot.its.jpo.geojsonconverter.GeoJsonConverterProperties;
import us.dot.its.jpo.geojsonconverter.converter.rtcm.RTCMConverter;
import us.dot.its.jpo.geojsonconverter.converter.srm.SrmConverter;
import us.dot.its.jpo.geojsonconverter.converter.ssm.SsmConverter;
import us.dot.its.jpo.geojsonconverter.validator.*;

@SpringBootTest(properties = "spring.kafka.streams.auto-startup=false")
@ActiveProfiles("test")
public class JsonConverterServiceControllerTest {
    JsonConverterServiceController geoJsonConverterServiceController;
    GeoJsonConverterProperties props;

    @Autowired
    MapJsonValidator mapJsonValidator;

    @Autowired
    SpatJsonValidator spatJsonValidator;

    @Autowired
    BsmJsonValidator bsmJsonValidator;

    @Autowired
    PsmJsonValidator psmJsonValidator;

    @Autowired
    RTCMJsonValidator rtcmJsonValidator;

    @Autowired
    RTCMConverter rtcmConverter;

    @Autowired
    TimJsonValidator timJsonValidator;

    @Autowired
    SrmJsonValidator srmJsonValidator;

    @Autowired
    SrmConverter srmConverter;

    @Autowired
    SsmJsonValidator ssmJsonValidator;

    @Autowired
    SsmConverter ssmConverter;

    @BeforeEach
    void setup() {
        props = new GeoJsonConverterProperties();
        props.initialize();
    }

    @Test
    public void testSpringBootLoaded() {
        geoJsonConverterServiceController = new JsonConverterServiceController(props, mapJsonValidator,
                spatJsonValidator, bsmJsonValidator, psmJsonValidator, rtcmJsonValidator, rtcmConverter,
                timJsonValidator, srmJsonValidator, srmConverter, ssmJsonValidator, ssmConverter);
        assertNotNull(geoJsonConverterServiceController);
    }
}
