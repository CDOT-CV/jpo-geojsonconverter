package us.dot.its.jpo.geojsonconverter.converter.tim;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.dot.its.jpo.geojsonconverter.TestResourceUtil.loadResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformationMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.common.Ieee1609Dot2SignedDataMetadata;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

class TimSerializationTest {

    @Test
    void convertsTimToReferenceProcessedJson() throws IOException {
        String timJson = loadResource("classpath:json/sample.ode-tim.json");
        String referenceProcessedTimJson = loadResource("classpath:json/sample.processed-tim.json");

        try (JsonDeserializer<OdeMessageFrameData> deserializer = new JsonDeserializer<>(OdeMessageFrameData.class)) {
            OdeMessageFrameData odeTim = deserializer.deserialize("test-topic", timJson.getBytes(StandardCharsets.UTF_8));
            TravelerInformationMessageFrame messageFrame = (TravelerInformationMessageFrame) odeTim.getPayload().getData();
            ProcessedTim processedTim = new TimConverter(new TimGeometryConverter())
                    .createProcessedTim(messageFrame.getValue(), odeTim.getMetadata());

            assertThatJson(referenceProcessedTimJson).isEqualTo(processedTim.toString());
        }
    }

    @Test
    void deserializesReferenceProcessedTimWithPathNodesOnly() throws IOException {
        String referenceProcessedTimJson = loadResource("classpath:json/sample.processed-tim.json");

        try (JsonDeserializer<ProcessedTim> deserializer = new JsonDeserializer<>(ProcessedTim.class)) {
            ProcessedTim processedTim = deserializer.deserialize("test-topic",
                    referenceProcessedTimJson.getBytes(StandardCharsets.UTF_8));

            assertEquals("TIM", processedTim.getMessageType());
            assertEquals(5, processedTim.getDataFrameFeatureCollection().getFeatures().size());
            MultiLineString geometry = assertInstanceOf(MultiLineString.class,
                    processedTim.getDataFrameFeatureCollection().getFeatures().getFirst().getGeometry());
            assertEquals(8, geometry.getCoordinates()[0].length);
            assertEquals(0, processedTim.getDataFrameFeatureCollection().getFeatures().getFirst().getProperties()
                    .getRegionInfoList().getFirst().getGeometryIndex());
        }
    }

    @Test
    void deserializesCertificatePresentProcessedTimWithValidityTimestamps() throws IOException {
        String certPresentProcessedTimJson = loadResource("classpath:json/sample.processed-tim-cert-present.json");

        try (JsonDeserializer<ProcessedTim> deserializer = new JsonDeserializer<>(ProcessedTim.class)) {
            ProcessedTim processedTim = deserializer.deserialize("test-topic",
                    certPresentProcessedTimJson.getBytes(StandardCharsets.UTF_8));

            assertTrue(processedTim.isCertPresent());
            Ieee1609Dot2SignedDataMetadata signedDataMetadata = processedTim.getSignedDataMetadata();
            assertNotNull(signedDataMetadata);
            assertEquals(131, signedDataMetadata.getPsid());
            assertEquals(Instant.parse("2026-06-10T23:59:05.885Z"), signedDataMetadata.getGenerationTime());
            assertEquals(Instant.parse("2026-06-04T19:42:18Z"), signedDataMetadata.getCertificateValidityStart());
            assertEquals(Instant.parse("2026-06-11T20:42:18Z"), signedDataMetadata.getCertificateValidityEnd());
        }
    }
}
