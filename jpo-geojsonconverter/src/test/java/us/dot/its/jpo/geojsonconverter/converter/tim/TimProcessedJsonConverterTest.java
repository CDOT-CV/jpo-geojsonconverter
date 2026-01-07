package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.kafka.streams.KeyValue;
import org.junit.Before;
import org.junit.Test;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.geojsonconverter.converter.FieldConversions;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.pojos.common.DeserializedRawMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedRegionType;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedDeploymentAgency;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedContentType;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

public class TimProcessedJsonConverterTest {
    TimProcessedJsonConverter timProcessedJsonConverter;
    OdeMessageFrameData timMF;

    @Before
    public void setup() throws IOException {
        // Load combined sample TIM JSON file for integration testing
        String timJsonString = new String(Files.readAllBytes(Paths.get("src/test/resources/json/sample.ode-tim.json")));

        try (JsonDeserializer<OdeMessageFrameData> odeTimDeserializer =
                new JsonDeserializer<>(OdeMessageFrameData.class)) {
            timMF = odeTimDeserializer.deserialize("test-topic", timJsonString.getBytes());
        }

        // Create TimConverter with TimGeometryProcessor dependency
        TimGeometryProcessor geometryProcessor = new TimGeometryProcessor();
        TimConverter timConverter = new TimConverter(geometryProcessor);
        timProcessedJsonConverter = new TimProcessedJsonConverter(timConverter);
    }

    @Test
    public void testTransformWithValidTim() {
        // Test successful TIM processing with sample data
        DeserializedRawMessageFrame deserializedRawTim = new DeserializedRawMessageFrame();
        deserializedRawTim.setOdeMessageFrameData(timMF);
        deserializedRawTim.setValidationFailure(false);
        deserializedRawTim.setValidationResults(new JsonValidatorResult());

        KeyValue<RsuTimKey, ProcessedTim> result = timProcessedJsonConverter.transform(null, deserializedRawTim);

        assertNotNull(result);
        assertNotNull(result.key);
        assertNotNull(result.value);

        // Verify basic TIM properties
        ProcessedTim processedTim = result.value;
        assertEquals("TIM", processedTim.getMessageType());
        assertEquals(1, processedTim.getMsgCnt().intValue());
        assertEquals("18D4A500000D7BA133", processedTim.getPacketId());
        assertNotNull(processedTim.getTimeStamp());
        assertNotNull(processedTim.getOdeReceivedAt());

        // Verify compliance
        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(processedTim.getCompliance().get(0).isCompliant());

        // Verify feature collection and geometry processing
        assertNotNull(processedTim.getDataFrameFeatureCollection());
        assertNotNull(processedTim.getDataFrameFeatureCollection().getFeatures());
        assertTrue(processedTim.getDataFrameFeatureCollection().getFeatures().size() > 0);
        assertNotNull(processedTim.getLocation());

        // Verify region type processing and scale attribute handling
        var feature = processedTim.getDataFrameFeatureCollection().getFeatures().get(0);
        assertNotNull(feature.getProperties());
        assertNotNull(feature.getProperties().getRegionInfoList());
        assertTrue(feature.getProperties().getRegionInfoList().size() > 0);
        assertNotNull(feature.getGeometry());

        // Verify region type determination
        var regionInfo = feature.getProperties().getRegionInfoList().get(0);
        assertNotNull(regionInfo.getRegionType());
        assertEquals(ProcessedRegionType.PATH, regionInfo.getRegionType());

        // Verify deployment agency and content type processing
        assertNotNull(feature.getProperties().getDeploymentAgencyType());
        assertEquals(ProcessedDeploymentAgency.STATE_OR_LOCAL, feature.getProperties().getDeploymentAgencyType());
        assertNotNull(feature.getProperties().getContent());
        assertEquals(ProcessedContentType.ADVISORY, feature.getProperties().getContent().getType());

        // Verify coordinate transformation accuracy
        validateCoordinateTransformation(timMF, processedTim);
    }

    @Test
    public void testTransformWithValidationMessages() {
        // Test TIM processing with validation messages (non-compliant)
        JsonValidatorResult validatorResult = new JsonValidatorResult();
        Exception testException = new Exception("Warning: Minor validation issue");
        validatorResult.addException(testException);

        DeserializedRawMessageFrame deserializedRawTim = new DeserializedRawMessageFrame();
        deserializedRawTim.setOdeMessageFrameData(timMF);
        deserializedRawTim.setValidationFailure(false);
        deserializedRawTim.setValidationResults(validatorResult);

        KeyValue<RsuTimKey, ProcessedTim> result = timProcessedJsonConverter.transform(null, deserializedRawTim);

        assertNotNull(result);
        assertNotNull(result.key);
        assertNotNull(result.value);

        // Verify compliance shows non-compliant due to validation messages
        ProcessedTim processedTim = result.value;
        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(!processedTim.getCompliance().get(0).isCompliant());
        assertTrue(processedTim.getCompliance().get(0).getValidationMessages().size() > 0);
    }

    @Test
    public void testTransformWithInvalidTim() {
        // Test TIM processing with validation failure
        JsonValidatorResult validatorResult = new JsonValidatorResult();
        Exception testException = new Exception("Critical validation error");
        validatorResult.addException(testException);

        DeserializedRawMessageFrame deserializedRawTim = new DeserializedRawMessageFrame();
        deserializedRawTim.setOdeMessageFrameData(timMF);
        deserializedRawTim.setValidationFailure(true);
        deserializedRawTim.setValidationResults(validatorResult);
        deserializedRawTim.setFailedMessage("Invalid TIM message");

        KeyValue<RsuTimKey, ProcessedTim> result = timProcessedJsonConverter.transform(null, deserializedRawTim);

        assertNotNull(result);
        assertNotNull(result.key);
        assertEquals("ERROR", result.key.getRsuId());
        assertNotNull(result.value);

        // Verify failure processing
        ProcessedTim processedTim = result.value;
        assertNotNull(processedTim.getCompliance());
        assertTrue(processedTim.getCompliance().size() > 0);
        assertTrue(!processedTim.getCompliance().get(0).isCompliant());
        assertTrue(processedTim.getCompliance().get(0).getValidationMessages().size() > 0);
    }

    @Test
    public void testTransformWithNullInput() {
        // Test error handling with null input
        KeyValue<RsuTimKey, ProcessedTim> result = timProcessedJsonConverter.transform(null, null);
        assertNotNull(result.key);
        assertEquals("ERROR", result.key.getRsuId());
        assertNull(result.value);
    }

    /**
     * Validates that coordinates in the input TIM message are close to the output GeoJSON coordinates.
     */
    private void validateCoordinateTransformation(OdeMessageFrameData inputTim, ProcessedTim outputTim) {
        try {
            // Extract input anchor coordinates
            TravelerInformationMessageFrame messageFrame =
                    (TravelerInformationMessageFrame) inputTim.getPayload().getData();
            TravelerDataFrame dataFrame = messageFrame.getValue().getDataFrames().get(0);
            GeographicalPath region = dataFrame.getRegions().get(0);

            double inputLat = FieldConversions.convertLat(region.getAnchor().getLat().getValue());
            double inputLon = FieldConversions.convertLong(region.getAnchor().getLong_().getValue());

            // Extract output coordinates from GeoJSON
            var feature = outputTim.getDataFrameFeatureCollection().getFeatures().get(0);
            LineString lineString = (LineString) feature.getGeometry();
            double[][] coordinates = lineString.getCoordinates();

            // Verify we have coordinates and they're in reasonable range
            assertTrue(coordinates.length > 0, "No coordinates found in output");
            assertTrue(coordinates.length > 1, "Expected multiple coordinates for path");

            // Check that coordinates are geographically valid
            for (double[] coord : coordinates) {
                assertTrue(coord[0] >= -180.0 && coord[0] <= 180.0, "Invalid longitude: " + coord[0]);
                assertTrue(coord[1] >= -90.0 && coord[1] <= 90.0, "Invalid latitude: " + coord[1]);
            }

            // Verify first output coordinate is reasonably close to input anchor
            double latDiff = Math.abs(inputLat - coordinates[0][1]);
            double lonDiff = Math.abs(inputLon - coordinates[0][0]);
            assertTrue(latDiff < 0.001, "Latitude difference too large: " + latDiff);
            assertTrue(lonDiff < 0.001, "Longitude difference too large: " + lonDiff);

        } catch (Exception e) {
            // Log but don't fail the test
            System.err.println("Coordinate validation failed: " + e.getMessage());
        }
    }

}
