package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ValidRegion;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIScodes;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ITIStextPhrase;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.SpeedLimit;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.SpeedLimitSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.WorkZone;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.WorkZoneSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GenericSignage;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GenericSignageSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ExitService;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.ExitServiceSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.MinutesDuration;
import us.dot.its.jpo.asn.j2735.r2024.Common.DYear;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;
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
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

class TimConverterTest {
    private TimConverter timConverter;
    private OdeMessageFrameData timMF;
    private TravelerInformation travelerInfo;

    @BeforeEach
    void setup() throws IOException {
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
    void testCreateProcessedTimWithValidData() {
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());

        assertNotNull(processedTim);
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getOdeReceivedAt());
        assertNotNull(processedTim.getOriginIp());
        assertNotNull(processedTim.getAsn1());

        assertEquals(1, processedTim.getMsgCnt().intValue());
        assertEquals("18D4A500000D7BA133", processedTim.getPacketId());

        assertNotNull(processedTim.getValidationMessages());
        assertTrue(processedTim.getValidationMessages().isEmpty());

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
    void testBestPracticePathUsesDirectionalityAndAnchor() {
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        ProcessedTimFeature<?> feature = processedTim.getDataFrameFeatureCollection().getFeatures().get(0);
        var regionInfo = feature.getProperties().getRegionInfoList().get(0);

        assertInstanceOf(ProcessedPathRegionInfo.class, regionInfo);
        assertNotNull(regionInfo.getAnchorPoint());
        assertNotNull(regionInfo.getAnchorPoint().getLatitude());
        assertNotNull(regionInfo.getAnchorPoint().getLongitude());
        assertNotNull(regionInfo.getAnchorPoint().getElevationMeters());
        assertEquals(regionInfo.getAnchorPoint().getElevationMeters(),
                regionInfo.getElevationProfile().getDefaultElevationMeters());

        assertInstanceOf(ProcessedDirectionalityDirectionInfo.class, regionInfo.getDirectionInfo());
        ProcessedDirectionalityDirectionInfo directionInfo =
                (ProcessedDirectionalityDirectionInfo) regionInfo.getDirectionInfo();
        assertEquals(ProcessedDirectionType.DIRECTIONALITY, directionInfo.getDirectionType());
        assertEquals(ProcessedDirectionality.FORWARD, directionInfo.getDirectionality());
    }

    @Test
    void testBestPracticeCircleUsesGeometryHeadingAndRadiusMeters() {
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
        // F00F selects bits 12-15 and 0-3, a 180-degree range centered on north.
        assertEquals(1, headingInfo.getHeadingList().size());
        assertEquals(0.0, headingInfo.getHeadingList().getFirst().getHeading(), 0.000001);
        assertEquals(180.0, headingInfo.getHeadingList().getFirst().getRange(), 0.000001);
        assertInstanceOf(Polygon.class, circleFeature.getGeometry());
    }

    @Test
    void testBestPracticePolygonUsesHeadingDirection() {
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        ProcessedTimFeature<?> polygonFeature = findFeatureWithRegionType(processedTim, ProcessedRegionType.POLYGON);
        assertNotNull(polygonFeature, "Sample TIM should include a polygon region");

        var regionInfo = polygonFeature.getProperties().getRegionInfoList().get(0);
        assertInstanceOf(ProcessedPolygonRegionInfo.class, regionInfo);
        assertInstanceOf(ProcessedHeadingDirectionInfo.class, regionInfo.getDirectionInfo());
        ProcessedHeadingDirectionInfo headingInfo = (ProcessedHeadingDirectionInfo) regionInfo.getDirectionInfo();
        assertEquals(ProcessedDirectionType.HEADING, headingInfo.getDirectionType());
        assertNotNull(headingInfo.getHeadingList());
        // 03C0 selects bits 6-9, a 90-degree range centered on south.
        assertEquals(1, headingInfo.getHeadingList().size());
        assertEquals(180.0, headingInfo.getHeadingList().getFirst().getHeading(), 0.000001);
        assertEquals(90.0, headingInfo.getHeadingList().getFirst().getRange(), 0.000001);
        assertInstanceOf(Polygon.class, polygonFeature.getGeometry());
    }

    @Test
    void testPolygonWithNonCompliantDirectionalityStillConverts() {
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
    void testValidityPeriodUsesOdeYearWhenStartYearMissing() {
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
    void testValidityStartHasMinutePrecisionIndependentOfReceiveSubseconds() {
        var metadata = timMF.getMetadata();
        metadata.setOdeReceivedAt("2025-10-07T21:40:19.711Z");
        ProcessedTim first = timConverter.createProcessedTim(travelerInfo, metadata);
        metadata.setOdeReceivedAt("2025-10-07T21:40:58.999Z");
        ProcessedTim second = timConverter.createProcessedTim(travelerInfo, metadata);

        var firstStart = first.getDataFrameFeatureCollection().getFeatures().get(0).getProperties()
                .getValidityPeriod().getStartTime();
        var secondStart = second.getDataFrameFeatureCollection().getFeatures().get(0).getProperties()
                .getValidityPeriod().getStartTime();
        assertEquals(firstStart, secondStart);
        assertEquals(0, firstStart.getSecond());
        assertEquals(0, firstStart.getNano());
    }

    @Test
    void testZeroStartYearUsesSameYearInferenceAsMissingYearAtNewYearBoundary() {
        var metadata = timMF.getMetadata();
        metadata.setOdeReceivedAt("2025-01-01T00:00:00.000Z");
        TravelerDataFrame frame = travelerInfo.getDataFrames().get(0);
        frame.setStartTime(new MinuteOfTheYear(525599));
        frame.setStartYear(new DYear(0));

        ProcessedTim zeroYear = timConverter.createProcessedTim(travelerInfo, metadata);
        var zeroYearStart = zeroYear.getDataFrameFeatureCollection().getFeatures().get(0).getProperties()
                .getValidityPeriod().getStartTime();

        frame.setStartYear(null);
        ProcessedTim missingYear = timConverter.createProcessedTim(travelerInfo, metadata);
        var missingYearStart = missingYear.getDataFrameFeatureCollection().getFeatures().get(0).getProperties()
                .getValidityPeriod().getStartTime();

        assertEquals(2024, zeroYearStart.getYear());
        assertEquals(zeroYearStart, missingYearStart);
    }

    @Test
    void testUnresolvableValidityStartRetainsFrameWithoutDerivedEndTime() {
        TravelerDataFrame frame = travelerInfo.getDataFrames().get(0);
        frame.setStartYear(new DYear(10000));
        frame.setStartTime(new MinuteOfTheYear(100));
        frame.setDurationTime(new MinutesDuration(10));

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());

        assertEquals(travelerInfo.getDataFrames().size(),
                processedTim.getDataFrameFeatureCollection().getFeatures().size());
        var validity = processedTim.getDataFrameFeatureCollection().getFeatures().get(0).getProperties()
                .getValidityPeriod();
        assertNull(validity.getStartTime());
        assertNull(validity.getEndTime());
        assertTrue(!validity.isInfinite());
    }

    @Test
    void testMixedPathAndCircleProducesGeometryCollection() {
        TravelerDataFrame pathFrame = findDataFrameWithRegionType(ProcessedRegionType.PATH);
        TravelerDataFrame circleFrame = findDataFrameWithRegionType(ProcessedRegionType.CIRCLE);
        assertNotNull(pathFrame);
        assertNotNull(circleFrame);

        GeographicalPath pathRegion = pathFrame.getRegions().get(0);
        GeographicalPath circleRegion = circleFrame.getRegions().get(0);

        TravelerDataFrame.SequenceOfRegions mixedRegions = new TravelerDataFrame.SequenceOfRegions();
        mixedRegions.add(circleRegion);
        mixedRegions.add(pathRegion);
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
        assertInstanceOf(Polygon.class, collection.getGeometries()[0]);
        assertInstanceOf(LineString.class, collection.getGeometries()[1]);
        assertInstanceOf(ProcessedCircleRegionInfo.class, feature.getProperties().getRegionInfoList().get(0));
        assertInstanceOf(ProcessedPathRegionInfo.class, feature.getProperties().getRegionInfoList().get(1));
        assertEquals(0, feature.getProperties().getRegionInfoList().get(0).getGeometryIndex());
        assertEquals(1, feature.getProperties().getRegionInfoList().get(1).getGeometryIndex());
    }

    @Test
    void testUnconvertibleRegionRetainsMetadataWithNullGeometryIndex() {
        TravelerDataFrame pathFrame = findDataFrameWithRegionType(ProcessedRegionType.PATH);
        GeographicalPath pathRegion = pathFrame.getRegions().getFirst();
        GeographicalPath unsupportedRegion = new GeographicalPath();
        GeographicalPath.DescriptionChoice description = new GeographicalPath.DescriptionChoice();
        description.setOldRegion(new ValidRegion());
        unsupportedRegion.setDescription(description);

        TravelerDataFrame.SequenceOfRegions regions = new TravelerDataFrame.SequenceOfRegions();
        regions.add(unsupportedRegion);
        regions.add(pathRegion);
        pathFrame.setRegions(regions);
        while (travelerInfo.getDataFrames().size() > 1) {
            travelerInfo.getDataFrames().removeLast();
        }

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        ProcessedTimFeature<?> feature = processedTim.getDataFrameFeatureCollection().getFeatures().getFirst();
        assertInstanceOf(LineString.class, feature.getGeometry());
        assertEquals(2, feature.getProperties().getRegionInfoList().size());
        assertNull(feature.getProperties().getRegionInfoList().get(0).getGeometryIndex());
        assertEquals(0, feature.getProperties().getRegionInfoList().get(1).getGeometryIndex());
    }

    @Test
    void testReorderedFramesKeepSourceIndexesAndPropertiesWhenMiddleGeometryIsUnavailable() {
        TravelerDataFrame circleFrame = findDataFrameWithRegionType(ProcessedRegionType.CIRCLE);
        TravelerDataFrame polygonFrame = findDataFrameWithRegionType(ProcessedRegionType.POLYGON);
        TravelerDataFrame pathFrame = findDataFrameWithRegionType(ProcessedRegionType.PATH);
        assertNotNull(circleFrame);
        assertNotNull(polygonFrame);
        assertNotNull(pathFrame);

        GeographicalPath unsupportedRegion = unsupportedRegion();
        TravelerDataFrame.SequenceOfRegions unsupportedRegions = new TravelerDataFrame.SequenceOfRegions();
        unsupportedRegions.add(unsupportedRegion);
        polygonFrame.setRegions(unsupportedRegions);

        circleFrame.setContent(createSpeedLimitContent());
        polygonFrame.setContent(createWorkZoneContent());
        pathFrame.setContent(createGenericSignContent());

        travelerInfo.getDataFrames().clear();
        travelerInfo.getDataFrames().add(circleFrame);
        travelerInfo.getDataFrames().add(polygonFrame);
        travelerInfo.getDataFrames().add(pathFrame);

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        var features = processedTim.getDataFrameFeatureCollection().getFeatures();
        assertEquals(travelerInfo.getDataFrames().size(), features.size());
        assertEquals(3, features.size());

        var circleFeature = features.get(0);
        assertEquals(0, circleFeature.getId());
        assertNotNull(circleFeature.getGeometry());
        assertEquals(ProcessedRegionType.CIRCLE,
                circleFeature.getProperties().getRegionInfoList().getFirst().getRegionType());
        assertEquals(ProcessedContentType.ROAD_SIGNAGE, circleFeature.getProperties().getContent().getType());
        assertEquals("speed-limit Slow down", circleFeature.getProperties().getContent().getSentence());

        var unavailableFeature = features.get(1);
        assertEquals(1, unavailableFeature.getId());
        assertNull(unavailableFeature.getGeometry());
        assertNotNull(unavailableFeature.getProperties().getValidityPeriod());
        assertEquals(1, unavailableFeature.getProperties().getRegionInfoList().size());
        assertEquals(ProcessedRegionType.UNKNOWN,
                unavailableFeature.getProperties().getRegionInfoList().getFirst().getRegionType());
        assertNull(unavailableFeature.getProperties().getRegionInfoList().getFirst().getGeometryIndex());
        assertEquals(ProcessedContentType.COMMERCIAL_SIGNAGE,
                unavailableFeature.getProperties().getContent().getType());
        assertEquals("speed-limit Work ahead", unavailableFeature.getProperties().getContent().getSentence());

        var pathFeature = features.get(2);
        assertEquals(2, pathFeature.getId());
        assertNotNull(pathFeature.getGeometry());
        assertEquals(ProcessedRegionType.PATH,
                pathFeature.getProperties().getRegionInfoList().getFirst().getRegionType());
        assertEquals(ProcessedContentType.GENERIC_SIGN, pathFeature.getProperties().getContent().getType());
        assertEquals("Turn left speed-limit", pathFeature.getProperties().getContent().getSentence());
    }

    @Test
    void testMixedRegionOrderKeepsGeometryIndexesAroundUnsupportedMiddleRegion() {
        TravelerDataFrame pathFrame = findDataFrameWithRegionType(ProcessedRegionType.PATH);
        TravelerDataFrame polygonFrame = findDataFrameWithRegionType(ProcessedRegionType.POLYGON);
        assertNotNull(pathFrame);
        assertNotNull(polygonFrame);

        GeographicalPath pathRegion = pathFrame.getRegions().getFirst();
        GeographicalPath polygonRegion = polygonFrame.getRegions().getFirst();
        TravelerDataFrame.SequenceOfRegions orderedRegions = new TravelerDataFrame.SequenceOfRegions();
        orderedRegions.add(pathRegion);
        orderedRegions.add(unsupportedRegion());
        orderedRegions.add(polygonRegion);
        pathFrame.setRegions(orderedRegions);

        int sourceFrameIndex = travelerInfo.getDataFrames().indexOf(pathFrame);
        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        var feature = processedTim.getDataFrameFeatureCollection().getFeatures().get(sourceFrameIndex);
        assertEquals(sourceFrameIndex, feature.getId());
        assertInstanceOf(GeometryCollection.class, feature.getGeometry());

        var regionInfo = feature.getProperties().getRegionInfoList();
        assertEquals(3, regionInfo.size());
        assertEquals(ProcessedRegionType.PATH, regionInfo.get(0).getRegionType());
        assertEquals(ProcessedRegionType.UNKNOWN, regionInfo.get(1).getRegionType());
        assertEquals(ProcessedRegionType.POLYGON, regionInfo.get(2).getRegionType());
        assertEquals(0, regionInfo.get(0).getGeometryIndex());
        assertNull(regionInfo.get(1).getGeometryIndex());
        assertEquals(1, regionInfo.get(2).getGeometryIndex());

        GeometryCollection geometry = (GeometryCollection) feature.getGeometry();
        assertEquals(2, geometry.getGeometries().length);
        assertInstanceOf(LineString.class, geometry.getGeometries()[0]);
        assertInstanceOf(Polygon.class, geometry.getGeometries()[1]);
    }

    private GeographicalPath unsupportedRegion() {
        GeographicalPath region = new GeographicalPath();
        GeographicalPath.DescriptionChoice description = new GeographicalPath.DescriptionChoice();
        description.setOldRegion(new ValidRegion());
        region.setDescription(description);
        return region;
    }

    @Test
    void testCreateFailureProcessedTim() {
        String failureMessage = "Test failure message";

        ProcessedTim processedTim = timConverter.createFailureProcessedTim(failureMessage);

        assertNotNull(processedTim);
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getValidationMessages());
        assertEquals(1, processedTim.getValidationMessages().size());
        assertEquals(failureMessage, processedTim.getValidationMessages().get(0).getMessage());
    }

    @Test
    void testJsonValidationMapsSchemaFailures() {
        ProcessedTim processedTim = new ProcessedTim();
        JsonValidatorResult validatorResult = new JsonValidatorResult();
        validatorResult.addException(new Exception("schema parse failure"));

        timConverter.jsonValidation(processedTim, validatorResult);

        assertEquals(1, processedTim.getValidationMessages().size());
        assertEquals("schema parse failure", processedTim.getValidationMessages().get(0).getMessage());
        assertNotNull(processedTim.getValidationMessages().get(0).getException());
    }

    @Test
    void testConvertsSpeedLimitAndWorkZoneContent() {
        assertConvertedContent(createSpeedLimitContent(), ProcessedContentType.ROAD_SIGNAGE, "speed-limit Slow down");
        assertConvertedContent(createWorkZoneContent(), ProcessedContentType.COMMERCIAL_SIGNAGE, "speed-limit Work ahead");
        assertConvertedContent(createGenericSignContent(), ProcessedContentType.GENERIC_SIGN, "Turn left speed-limit");
        assertConvertedContent(createExitServiceContent(), ProcessedContentType.EXIT_SERVICE, "speed-limit Exit ahead");
    }

    private void assertConvertedContent(TravelerDataFrame.ContentChoice content, ProcessedContentType expectedType,
            String expectedSentence) {
        travelerInfo.getDataFrames().get(0).setContent(content);

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfo, timMF.getMetadata());
        var convertedContent = processedTim.getDataFrameFeatureCollection().getFeatures().get(0).getProperties()
                .getContent();

        assertEquals(expectedType, convertedContent.getType());
        assertEquals(2, convertedContent.getContentItems().size());
        assertEquals(expectedSentence, convertedContent.getSentence());
    }

    private TravelerDataFrame.ContentChoice createSpeedLimitContent() {
        SpeedLimit speedLimit = new SpeedLimit();
        SpeedLimitSequence itisSequence = new SpeedLimitSequence();
        SpeedLimitSequence.ItemChoice itisItem = new SpeedLimitSequence.ItemChoice();
        itisItem.setItis(new ITIScodes(268));
        itisSequence.setItem(itisItem);
        speedLimit.add(itisSequence);

        SpeedLimitSequence textSequence = new SpeedLimitSequence();
        SpeedLimitSequence.ItemChoice textItem = new SpeedLimitSequence.ItemChoice();
        textItem.setText(new ITIStextPhrase("Slow down"));
        textSequence.setItem(textItem);
        speedLimit.add(textSequence);

        TravelerDataFrame.ContentChoice content = new TravelerDataFrame.ContentChoice();
        content.setSpeedLimit(speedLimit);
        return content;
    }

    private TravelerDataFrame.ContentChoice createWorkZoneContent() {
        WorkZone workZone = new WorkZone();
        WorkZoneSequence itisSequence = new WorkZoneSequence();
        WorkZoneSequence.ItemChoice itisItem = new WorkZoneSequence.ItemChoice();
        itisItem.setItis(new ITIScodes(268));
        itisSequence.setItem(itisItem);
        workZone.add(itisSequence);

        WorkZoneSequence textSequence = new WorkZoneSequence();
        WorkZoneSequence.ItemChoice textItem = new WorkZoneSequence.ItemChoice();
        textItem.setText(new ITIStextPhrase("Work ahead"));
        textSequence.setItem(textItem);
        workZone.add(textSequence);

        TravelerDataFrame.ContentChoice content = new TravelerDataFrame.ContentChoice();
        content.setWorkZone(workZone);
        return content;
    }

    private TravelerDataFrame.ContentChoice createGenericSignContent() {
        GenericSignage genericSign = new GenericSignage();
        GenericSignageSequence textSequence = new GenericSignageSequence();
        GenericSignageSequence.ItemChoice textItem = new GenericSignageSequence.ItemChoice();
        textItem.setText(new ITIStextPhrase("Turn left"));
        textSequence.setItem(textItem);
        genericSign.add(textSequence);

        GenericSignageSequence itisSequence = new GenericSignageSequence();
        GenericSignageSequence.ItemChoice itisItem = new GenericSignageSequence.ItemChoice();
        itisItem.setItis(new ITIScodes(268));
        itisSequence.setItem(itisItem);
        genericSign.add(itisSequence);

        TravelerDataFrame.ContentChoice content = new TravelerDataFrame.ContentChoice();
        content.setGenericSign(genericSign);
        return content;
    }

    private TravelerDataFrame.ContentChoice createExitServiceContent() {
        ExitService exitService = new ExitService();
        ExitServiceSequence itisSequence = new ExitServiceSequence();
        ExitServiceSequence.ItemChoice itisItem = new ExitServiceSequence.ItemChoice();
        itisItem.setItis(new ITIScodes(268));
        itisSequence.setItem(itisItem);
        exitService.add(itisSequence);

        ExitServiceSequence textSequence = new ExitServiceSequence();
        ExitServiceSequence.ItemChoice textItem = new ExitServiceSequence.ItemChoice();
        textItem.setText(new ITIStextPhrase("Exit ahead"));
        textSequence.setItem(textItem);
        exitService.add(textSequence);

        TravelerDataFrame.ContentChoice content = new TravelerDataFrame.ContentChoice();
        content.setExitService(exitService);
        return content;
    }

    private static final Long KNOWN_ITIS_CODE_1 = 268L;
    private static final Long UNKNOWN_ITIS_CODE = 999999L;
    private static final Long NEGATIVE_ITIS_CODE = -1L;
    private static final Long ZERO_ITIS_CODE = 0L;

    @Test
    void testLookupItisCodeWithValidCode() {
        String result = TimConverter.lookupItisCode(KNOWN_ITIS_CODE_1);
        assertEquals("speed-limit", result);
    }

    @Test
    void testLookupItisCodeWithUnknownCode() {
        String result = TimConverter.lookupItisCode(UNKNOWN_ITIS_CODE);
        assertEquals("unknown", result);
    }

    @Test
    void testLookupItisCodeWithNullCode() {
        String result = TimConverter.lookupItisCode(null);
        assertEquals("unknown", result);
    }

    @Test
    void testLookupItisCodeWithNegativeCode() {
        String result = TimConverter.lookupItisCode(NEGATIVE_ITIS_CODE);
        assertEquals("unknown", result);
    }

    @Test
    void testLookupItisCodeWithZeroCode() {
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
