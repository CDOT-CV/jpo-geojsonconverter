package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformationMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedContentType;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedRegionType;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

public class TimConverterTest {
    private TimConverter timConverter;
    private OdeMessageFrameData timMF;

    @BeforeEach
    public void setup() throws IOException {
        // Load sample TIM JSON file
        String timJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/sample.ode-tim.json")));

        try (JsonDeserializer<OdeMessageFrameData> odeTimDeserializer =
                new JsonDeserializer<>(OdeMessageFrameData.class)) {
            timMF = odeTimDeserializer.deserialize("test-topic", timJsonString.getBytes());
        }

        TimGeometryConverter geometryProcessor = new TimGeometryConverter();
        timConverter = new TimConverter(geometryProcessor);
    }

    @Test
    public void testCreateProcessedTimWithValidData() {
        // Extract ASN.1 data
        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        TravelerInformation travelerInfo = messageFrame.getValue();

        // Test successful TIM creation
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());

        // Verify basic properties
        assertNotNull(processedTim);
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getOdeReceivedAt());
        assertNotNull(processedTim.getOriginIp());
        assertNotNull(processedTim.getAsn1());

        // Verify TIM-specific properties
        assertEquals(1, processedTim.getMsgCnt().intValue());
        assertEquals("18D4A500000D7BA133", processedTim.getPacketId());

        // Verify compliance
        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(processedTim.getCompliance().get(0).isCompliant());

        // Verify feature collection
        assertNotNull(processedTim.getDataFrameFeatureCollection());
        assertNotNull(processedTim.getDataFrameFeatureCollection().getFeatures());
        assertTrue(processedTim.getDataFrameFeatureCollection().getFeatures().size() > 0);

        // Verify location calculation
        assertNotNull(processedTim.getLocation());

        // Verify feature properties
        var feature = processedTim.getDataFrameFeatureCollection().getFeatures().get(0);
        assertNotNull(feature.getProperties());
        assertNotNull(feature.getProperties().getRegionInfoList());
        assertTrue(feature.getProperties().getRegionInfoList().size() > 0);
        assertNotNull(feature.getGeometry());

        // Verify region type
        var regionInfo = feature.getProperties().getRegionInfoList().get(0);
        assertNotNull(regionInfo.getRegionType());
        assertEquals(ProcessedRegionType.PATH, regionInfo.getRegionType());

        // Verify content processing
        assertNotNull(feature.getProperties().getContent());
        assertEquals(ProcessedContentType.ADVISORY, feature.getProperties().getContent().getType());
        assertNotNull(feature.getProperties().getContent().getContentItems());
        assertTrue(feature.getProperties().getContent().getContentItems().size() > 0);
    }

    @Test
    public void testCreateFailureProcessedTim() {
        String failureMessage = "Test failure message";

        ProcessedTim processedTim = timConverter.createFailureProcessedTim(failureMessage);

        // Verify failure processing
        assertNotNull(processedTim);
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(!processedTim.getCompliance().get(0).isCompliant());
    }

    // Known ITIS codes for testing (these should be valid ITIS codes)
    private static final Long KNOWN_ITIS_CODE_1 = 268L; // Should be a valid ITIS code
    private static final Long UNKNOWN_ITIS_CODE = 999999L; // Should be an unknown ITIS code
    private static final Long NEGATIVE_ITIS_CODE = -1L; // Invalid ITIS code
    private static final Long ZERO_ITIS_CODE = 0L; // Zero ITIS code

    @Test
    public void testLookupItisCodeWithValidCode() {
        String result = TimConverter.lookupItisCode(KNOWN_ITIS_CODE_1);
        assertEquals("speed-limit", result);
    }

    @Test
    public void testLookupItisCodeWithUnknownCode() {
        String result = TimConverter.lookupItisCode(UNKNOWN_ITIS_CODE);
        assertEquals("unknown", result);
    }

    @Test
    public void testLookupItisCodeWithNullCode() {
        String result = TimConverter.lookupItisCode(null);
        assertEquals("unknown", result);
    }

    @Test
    public void testLookupItisCodeWithNegativeCode() {
        String result = TimConverter.lookupItisCode(NEGATIVE_ITIS_CODE);
        assertEquals("unknown", result);
    }

    @Test
    public void testLookupItisCodeWithZeroCode() {
        String result = TimConverter.lookupItisCode(ZERO_ITIS_CODE);
        assertEquals("unknown", result);
    }
}
