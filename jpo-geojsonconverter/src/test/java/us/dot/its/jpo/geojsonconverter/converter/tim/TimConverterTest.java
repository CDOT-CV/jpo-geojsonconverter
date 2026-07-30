package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DirectionOfUse;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformationMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.GeometryCollection;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedCircleRegionInfo;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedContentType;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedDirectionType;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedDirectionality;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedDirectionalityDirectionInfo;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedHeadingDirectionInfo;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedPathRegionInfo;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedPolygonRegionInfo;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedRegionType;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedTimFeature;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

public class TimConverterTest {
    private TimConverter timConverter;
    private OdeMessageFrameData timMF;
    private TravelerInformation travelerInfo;

    @BeforeEach
    public void setup() throws IOException {
        // Load sample TIM JSON file
        String timJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/sample.ode-tim.json")));

        try (JsonDeserializer<OdeMessageFrameData> odeTimDeserializer =
                new JsonDeserializer<>(OdeMessageFrameData.class)) {
            timMF = odeTimDeserializer.deserialize("test-topic", timJsonString.getBytes());
        }

        TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) timMF.getPayload().getData();
        travelerInfo = messageFrame.getValue();

        TimGeometryConverter geometryProcessor = new TimGeometryConverter();
        timConverter = new TimConverter(geometryProcessor);
    }

    @Test
    public void testCreateProcessedTimWithValidData() {
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());

        assertNotNull(processedTim);
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getOdeReceivedAt());
        assertNotNull(processedTim.getOriginIp());
        assertNotNull(processedTim.getAsn1());

        assertEquals(1, processedTim.getMsgCnt().intValue());
        assertEquals("18D4A500000D7BA133", processedTim.getPacketId());

        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(processedTim.getCompliance().get(0).isCompliant());

        assertNotNull(processedTim.getDataFrameFeatureCollection());
        assertNotNull(processedTim.getDataFrameFeatureCollection().getFeatures());
        assertTrue(processedTim.getDataFrameFeatureCollection().getFeatures().size() > 0);

        assertNotNull(processedTim.getLocation());

        var feature = processedTim.getDataFrameFeatureCollection().getFeatures().get(0);
        assertNotNull(feature.getProperties());
        assertNotNull(feature.getProperties().getRegionInfoList());
        assertTrue(feature.getProperties().getRegionInfoList().size() > 0);
        assertNotNull(feature.getGeometry());

        var regionInfo = feature.getProperties().getRegionInfoList().get(0);
        assertNotNull(regionInfo.getRegionType());
        assertEquals(ProcessedRegionType.PATH, regionInfo.getRegionType());

        assertNotNull(feature.getProperties().getContent());
        assertEquals(ProcessedContentType.ADVISORY, feature.getProperties().getContent().getType());
        assertNotNull(feature.getProperties().getContent().getContentItems());
        assertTrue(feature.getProperties().getContent().getContentItems().size() > 0);
    }

    @Test
    public void testBestPracticePathUsesDirectionalityAndAnchor() {
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        ProcessedTimFeature<?> feature = processedTim.getDataFrameFeatureCollection().getFeatures().get(0);
        var regionInfo = feature.getProperties().getRegionInfoList().get(0);

        assertInstanceOf(ProcessedPathRegionInfo.class, regionInfo);
        assertNotNull(regionInfo.getAnchorPoint());
        assertNotNull(regionInfo.getAnchorPoint().getLatitude());
        assertNotNull(regionInfo.getAnchorPoint().getLongitude());

        assertInstanceOf(ProcessedDirectionalityDirectionInfo.class, regionInfo.getDirectionInfo());
        ProcessedDirectionalityDirectionInfo directionInfo =
                (ProcessedDirectionalityDirectionInfo) regionInfo.getDirectionInfo();
        assertEquals(ProcessedDirectionType.DIRECTIONALITY, directionInfo.getDirectionType());
        assertEquals(ProcessedDirectionality.FORWARD, directionInfo.getDirectionality());
    }

    @Test
    public void testBestPracticeCircleUsesGeometryHeadingAndRadiusMeters() {
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        ProcessedTimFeature<?> circleFeature = findFeatureWithRegionType(processedTim, ProcessedRegionType.CIRCLE);
        assertNotNull(circleFeature, "Sample TIM should include a circle region");

        var regionInfo = circleFeature.getProperties().getRegionInfoList().get(0);
        assertInstanceOf(ProcessedCircleRegionInfo.class, regionInfo);
        ProcessedCircleRegionInfo circleInfo = (ProcessedCircleRegionInfo) regionInfo;

        // Sample circle: radius 1001 decimeters => exactly 100.1 m.
        assertEquals(100.1, circleInfo.getRadius(), 0.000001);

        assertInstanceOf(ProcessedHeadingDirectionInfo.class, circleInfo.getDirectionInfo());
        ProcessedHeadingDirectionInfo headingInfo = (ProcessedHeadingDirectionInfo) circleInfo.getDirectionInfo();
        assertEquals(ProcessedDirectionType.HEADING, headingInfo.getDirectionType());
        assertNotNull(headingInfo.getHeadingList());
        assertTrue(headingInfo.getHeadingList().size() > 0);
        assertInstanceOf(Polygon.class, circleFeature.getGeometry());
    }

    @Test
    public void testBestPracticePolygonUsesHeadingDirection() {
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        ProcessedTimFeature<?> polygonFeature = findFeatureWithRegionType(processedTim, ProcessedRegionType.POLYGON);
        assertNotNull(polygonFeature, "Sample TIM should include a polygon region");

        var regionInfo = polygonFeature.getProperties().getRegionInfoList().get(0);
        assertInstanceOf(ProcessedPolygonRegionInfo.class, regionInfo);
        assertInstanceOf(ProcessedHeadingDirectionInfo.class, regionInfo.getDirectionInfo());
        ProcessedHeadingDirectionInfo headingInfo = (ProcessedHeadingDirectionInfo) regionInfo.getDirectionInfo();
        assertEquals(ProcessedDirectionType.HEADING, headingInfo.getDirectionType());
        assertNotNull(headingInfo.getHeadingList());
        assertTrue(headingInfo.getHeadingList().size() > 0);
        assertInstanceOf(Polygon.class, polygonFeature.getGeometry());
    }

    @Test
    public void testPolygonWithNonCompliantDirectionalityStillConverts() {
        // Regression: preferring ASN directionality for polygons previously threw and dropped the feature
        TravelerDataFrame polygonFrame = findDataFrameWithRegionType(ProcessedRegionType.POLYGON);
        assertNotNull(polygonFrame);
        GeographicalPath polygonRegion = polygonFrame.getRegions().get(0);
        polygonRegion.setDirectionality(DirectionOfUse.BOTH);

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        ProcessedTimFeature<?> polygonFeature = findFeatureWithRegionType(processedTim, ProcessedRegionType.POLYGON);

        assertNotNull(polygonFeature, "Polygon feature must not be dropped when directionality is also present");
        var regionInfo = polygonFeature.getProperties().getRegionInfoList().get(0);
        assertInstanceOf(ProcessedHeadingDirectionInfo.class, regionInfo.getDirectionInfo());
    }

    @Test
    public void testValidityPeriodUsesOdeYearWhenStartYearMissing() {
        TravelerDataFrame frame = travelerInfo.getDataFrames().get(0);
        frame.setStartYear(null);

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        var validity = processedTim.getDataFrameFeatureCollection().getFeatures().get(0).getProperties()
                .getValidityPeriod();

        assertNotNull(validity);
        assertNotNull(validity.getStartTime());
        assertEquals(2025, validity.getStartTime().getYear());
        assertTrue(validity.isInfinite());
    }

    @Test
    public void testMixedPathAndCircleProducesGeometryCollection() {
        TravelerDataFrame pathFrame = findDataFrameWithRegionType(ProcessedRegionType.PATH);
        TravelerDataFrame circleFrame = findDataFrameWithRegionType(ProcessedRegionType.CIRCLE);
        assertNotNull(pathFrame);
        assertNotNull(circleFrame);

        GeographicalPath pathRegion = pathFrame.getRegions().get(0);
        GeographicalPath circleRegion = circleFrame.getRegions().get(0);

        TravelerDataFrame.SequenceOfRegions mixedRegions = new TravelerDataFrame.SequenceOfRegions();
        mixedRegions.add(pathRegion);
        mixedRegions.add(circleRegion);
        pathFrame.setRegions(mixedRegions);

        // Keep a single data frame for a focused assertion
        while (travelerInfo.getDataFrames().size() > 1) {
            travelerInfo.getDataFrames().remove(travelerInfo.getDataFrames().size() - 1);
        }

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        assertEquals(1, processedTim.getDataFrameFeatureCollection().getFeatures().size());

        ProcessedTimFeature<?> feature = processedTim.getDataFrameFeatureCollection().getFeatures().get(0);
        assertEquals(2, feature.getProperties().getRegionInfoList().size());
        assertInstanceOf(GeometryCollection.class, feature.getGeometry());

        GeometryCollection collection = (GeometryCollection) feature.getGeometry();
        assertEquals(2, collection.getGeometries().length);
        assertTrue(collection.getGeometries()[0] instanceof LineString
                || collection.getGeometries()[1] instanceof LineString);
        assertTrue(collection.getGeometries()[0] instanceof Polygon || collection.getGeometries()[1] instanceof Polygon);
    }

    @Test
    public void testCreateFailureProcessedTim() {
        String failureMessage = "Test failure message";

        ProcessedTim processedTim = timConverter.createFailureProcessedTim(failureMessage);

        assertNotNull(processedTim);
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(!processedTim.getCompliance().get(0).isCompliant());
    }

    private static final Long KNOWN_ITIS_CODE_1 = 268L;
    private static final Long UNKNOWN_ITIS_CODE = 999999L;
    private static final Long NEGATIVE_ITIS_CODE = -1L;
    private static final Long ZERO_ITIS_CODE = 0L;

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

    private ProcessedTimFeature<?> findFeatureWithRegionType(ProcessedTim processedTim, ProcessedRegionType type) {
        for (var feature : processedTim.getDataFrameFeatureCollection().getFeatures()) {
            if (feature.getProperties() != null && feature.getProperties().getRegionInfoList() != null) {
                for (var regionInfo : feature.getProperties().getRegionInfoList()) {
                    if (type.equals(regionInfo.getRegionType())) {
                        return feature;
                    }
                }
            }
        }
        return null;
    }

    private TravelerDataFrame findDataFrameWithRegionType(ProcessedRegionType type) {
        for (TravelerDataFrame frame : travelerInfo.getDataFrames()) {
            if (frame.getRegions() == null) {
                continue;
            }
            for (GeographicalPath region : frame.getRegions()) {
                boolean isCircle = region.getDescription() != null && region.getDescription().getGeometry() != null
                        && region.getDescription().getGeometry().getCircle() != null;
                boolean isClosed = region.getClosedPath() != null && region.getClosedPath().getValue();
                boolean hasPath = region.getDescription() != null && region.getDescription().getPath() != null;

                ProcessedRegionType regionType;
                if (isCircle) {
                    regionType = ProcessedRegionType.CIRCLE;
                } else if (isClosed) {
                    regionType = ProcessedRegionType.POLYGON;
                } else if (hasPath) {
                    regionType = ProcessedRegionType.PATH;
                } else {
                    regionType = ProcessedRegionType.UNKNOWN;
                }

                if (type.equals(regionType)) {
                    return frame;
                }
            }
        }
        return null;
    }
}
