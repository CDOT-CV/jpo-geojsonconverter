package us.dot.its.jpo.geojsonconverter.pojos.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.junit.jupiter.api.Test;

class Ieee1609Dot2MetadataExtractorTest {

    @Test
    void extractsSignedDataMetadataFromOdeFrame() throws IOException {
        Ieee1609Dot2SignedDataMetadata result =
                Ieee1609Dot2MetadataExtractor.extractSignedDataMetadata(loadResource("/json/signed-data-metadata.json"));

        assertEquals(32, result.getPsid());
        assertEquals(Instant.parse("2026-05-15T18:30:13.894Z"), result.getGenerationTime());
        assertEquals(Instant.parse("2026-05-14T10:28:01Z"), result.getCertificateValidityStart());
        assertEquals(Instant.parse("2026-06-11T11:28:01Z"), result.getCertificateValidityEnd());
    }

    @Test
    void returnsNullWhenSignedDataMetadataIsAbsentOrNull() throws IOException {
        assertNull(Ieee1609Dot2MetadataExtractor.extractSignedDataMetadata("{\"metadata\":{}}".getBytes()));
        assertNull(Ieee1609Dot2MetadataExtractor
                .extractSignedDataMetadata("{\"metadata\":{\"signedDataMetadata\":null}}".getBytes()));
    }

    @Test
    void rejectsMalformedJson() {
        assertThrows(IOException.class,
                () -> Ieee1609Dot2MetadataExtractor.extractSignedDataMetadata("not-json".getBytes()));
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
