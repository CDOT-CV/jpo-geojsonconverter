package us.dot.its.jpo.geojsonconverter.pojos.common;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dot.its.jpo.geojsonconverter.DateJsonMapper;

/**
 * Extracts IEEE 1609.2 fields from raw ODE message-frame JSON.
 *
 * <p>The pinned ODE model does not yet expose {@code signedDataMetadata}, so
 * extracting it before deserialization prevents the unknown JSON field from
 * being lost. The extractor is shared by message-type-specific topologies.
 */
public final class Ieee1609Dot2MetadataExtractor {
    private static final ObjectMapper OBJECT_MAPPER = DateJsonMapper.getInstance();

    private Ieee1609Dot2MetadataExtractor() {
    }

    /**
     * Gets the optional signed-data metadata nested under an ODE frame's
     * {@code metadata} property.
     *
     * @param odeMessageFrameJson raw ODE message-frame JSON
     * @return signed-data metadata, or {@code null} when the message is unsigned
     *         or the ODE frame does not provide the field
     * @throws IOException if the provided data is not valid JSON
     */
    public static Ieee1609Dot2SignedDataMetadata extractSignedDataMetadata(byte[] odeMessageFrameJson)
            throws IOException {
        JsonNode root = OBJECT_MAPPER.readTree(odeMessageFrameJson);
        JsonNode signedDataMetadata = root.path("metadata").path("signedDataMetadata");

        if (signedDataMetadata.isMissingNode() || signedDataMetadata.isNull()) {
            return null;
        }

        return OBJECT_MAPPER.treeToValue(signedDataMetadata, Ieee1609Dot2SignedDataMetadata.class);
    }
}
