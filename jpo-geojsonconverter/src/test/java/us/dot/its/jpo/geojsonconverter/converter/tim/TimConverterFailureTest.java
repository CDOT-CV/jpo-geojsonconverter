package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static us.dot.its.jpo.geojsonconverter.TestResourceUtil.loadResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformationMessageFrame;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.common.DeserializedRawMessageFrame;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.geojsonconverter.validator.AbstractJsonValidator;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

class TimConverterFailureTest {
    private OdeMessageFrameData timMessageFrame;
    private TravelerInformation travelerInfo;

    @BeforeEach
    void setup() throws IOException {
        String json = loadResource("classpath:json/sample.ode-tim.json");
        try (JsonDeserializer<OdeMessageFrameData> deserializer = new JsonDeserializer<>(OdeMessageFrameData.class)) {
            timMessageFrame = deserializer.deserialize("test-topic", json.getBytes(StandardCharsets.UTF_8));
        }
        TravelerInformationMessageFrame messageFrame =
                (TravelerInformationMessageFrame) timMessageFrame.getPayload().getData();
        travelerInfo = messageFrame.getValue();
        while (travelerInfo.getDataFrames().size() > 3) {
            travelerInfo.getDataFrames().removeLast();
        }
        assertEquals(3, travelerInfo.getDataFrames().size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2})
    void geometryFailureRetainsFramePropertiesPositionsAndDiagnosticsThroughTransformer(int failedFrame) throws Exception {
        var expected = new TimConverter(new TimGeometryConverter())
                .createProcessedTim(travelerInfo, timMessageFrame.getMetadata());
        TimGeometryConverter geometryProcessor = spy(new TimGeometryConverter());
        doThrow(new IllegalStateException("geometry conversion failed"))
                .when(geometryProcessor).createGeometryResultFromDataFrame(any(TravelerDataFrame.class), eq(failedFrame));

        DeserializedRawMessageFrame rawTim = new DeserializedRawMessageFrame();
        rawTim.setOdeMessageFrameData(timMessageFrame);
        JsonValidatorResult validationResults = new JsonValidatorResult();
        validationResults.addException(new Exception("existing validation failure"));
        rawTim.setValidationResults(validationResults);
        var result = new TimTransformer(new TimConverter(geometryProcessor)).apply(null, rawTim);

        assertNotNull(result.value);
        var processedTim = result.value;
        assertEquals(timMessageFrame.getMetadata().getOriginIp(), result.key.getRsuId());
        var features = processedTim.getDataFrameFeatureCollection().getFeatures();
        assertEquals(3, features.size());
        for (int i = 0; i < features.size(); i++) {
            var feature = features.get(i);
            assertEquals(i, feature.getId());
            assertEquals(expected.getDataFrameFeatureCollection().getFeatures().get(i).getProperties().getContent(),
                    feature.getProperties().getContent());
            if (i == failedFrame) {
                assertNull(feature.getGeometry());
                assertNotNull(feature.getProperties().getValidityPeriod());
                feature.getProperties().getRegionInfoList().forEach(region -> assertNull(region.getGeometryIndex()));
            } else {
                assertEquals(DateJsonMapper.getInstance()
                        .valueToTree(expected.getDataFrameFeatureCollection().getFeatures().get(i).getGeometry()),
                        DateJsonMapper.getInstance().valueToTree(feature.getGeometry()));
                assertEquals(expected.getDataFrameFeatureCollection().getFeatures().get(i).getProperties(),
                        feature.getProperties());
            }
        }

        assertEquals(2, processedTim.getValidationMessages().size());
        var conversionFailure = processedTim.getValidationMessages().getFirst();
        assertEquals("$.dataFrames[" + failedFrame + "]", conversionFailure.getJsonPath());
        assertTrue(conversionFailure.getMessage().contains("geometry conversion failed"));
        assertNotNull(conversionFailure.getException());
        assertEquals("existing validation failure", processedTim.getValidationMessages().get(1).getMessage());

        String output = processedTim.toString();
        var json = DateJsonMapper.getInstance().readTree(output);
        var failedFeature = json.at("/dataFrameFeatureCollection/features/" + failedFrame);
        assertEquals("Feature", failedFeature.path("type").asText());
        assertEquals(failedFrame, failedFeature.path("id").asInt());
        assertTrue(failedFeature.has("geometry"));
        assertTrue(failedFeature.path("geometry").isNull());
        assertEquals("$.dataFrames[" + failedFrame + "]", json.at("/validationMessages/0/jsonPath").asText());
        AbstractJsonValidator validator = new AbstractJsonValidator("classpath:schemas/processed-tim.schema.json") {
        };
        assertTrue(validator.validate(output).isValid());
    }

    @Test
    void missingMiddleFrameRetainsAnEmptyFeatureAndContinuesConvertingLaterFrames() {
        travelerInfo.getDataFrames().set(1, null);
        var processedTim = new TimConverter(new TimGeometryConverter())
                .createProcessedTim(travelerInfo, timMessageFrame.getMetadata());

        var features = processedTim.getDataFrameFeatureCollection().getFeatures();
        assertEquals(3, features.size());
        for (int i = 0; i < features.size(); i++) {
            assertEquals(i, features.get(i).getId());
        }
        assertNotNull(features.get(0).getGeometry());
        assertNull(features.get(1).getGeometry());
        assertNotNull(features.get(1).getProperties());
        assertNull(features.get(1).getProperties().getContent());
        assertNotNull(features.get(2).getGeometry());
        assertNotNull(features.get(2).getProperties().getContent());
        assertEquals(1, processedTim.getValidationMessages().size());
        assertEquals("$.dataFrames[1]", processedTim.getValidationMessages().getFirst().getJsonPath());
    }

    @Test
    void collectionFailureProducesAnEmptyCollectionWithAVisibleDiagnostic() {
        TravelerInformation brokenTim = spy(travelerInfo);
        doThrow(new IllegalStateException("data frames cannot be read")).when(brokenTim).getDataFrames();
        var processedTim = new TimConverter(new TimGeometryConverter())
                .createProcessedTim(brokenTim, timMessageFrame.getMetadata());

        assertTrue(processedTim.getDataFrameFeatureCollection().getFeatures().isEmpty());
        assertEquals(1, processedTim.getValidationMessages().size());
        var failure = processedTim.getValidationMessages().getFirst();
        assertEquals("$.dataFrames", failure.getJsonPath());
        assertTrue(failure.getMessage().contains("data frames cannot be read"));
        assertNotNull(failure.getException());
        assertEquals(timMessageFrame.getMetadata().getOriginIp(), processedTim.getOriginIp());
    }

    @Test
    void locationFailureOmitsLocationAndKeepsTheRestOfTheMessage() {
        TimGeometryConverter geometryProcessor = spy(new TimGeometryConverter());
        doThrow(new IllegalStateException("location failed")).when(geometryProcessor)
                .calculateRepresentativeLocation(any(TravelerInformation.class));

        var processedTim = new TimConverter(geometryProcessor)
                .createProcessedTim(travelerInfo, timMessageFrame.getMetadata());

        assertNull(processedTim.getLocation());
        assertEquals(3, processedTim.getDataFrameFeatureCollection().getFeatures().size());
        assertTrue(processedTim.getValidationMessages().isEmpty());
    }

    @Test
    void absentDataFramesProduceAnEmptyCollectionWithoutAConversionFailure() {
        travelerInfo.setDataFrames(null);
        var processedTim = new TimConverter(new TimGeometryConverter())
                .createProcessedTim(travelerInfo, timMessageFrame.getMetadata());

        assertTrue(processedTim.getDataFrameFeatureCollection().getFeatures().isEmpty());
        assertTrue(processedTim.getValidationMessages().isEmpty());
    }
}
