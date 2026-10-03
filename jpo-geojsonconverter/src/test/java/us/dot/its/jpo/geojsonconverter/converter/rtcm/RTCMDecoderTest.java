package us.dot.its.jpo.geojsonconverter.converter.rtcm;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class RTCMDecoderTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5})
    void partialDecodeReturnsEmptyObjectWhenHeaderIsTooShort(int length) {
        JsonNode decoded = RTCMDecoder.partialDecode(new byte[length]);

        assertTrue(decoded.isObject());
        assertEquals(0, decoded.size());
    }

    @Test
    void partialDecodeReturnsEmptyObjectForInvalidPreamble() {
        JsonNode decoded = RTCMDecoder.partialDecode(new byte[] {0, 0, 0, 0, 0, 0});

        assertEquals(0, decoded.size());
    }

    @Test
    void partialDecodeReturnsOnlyClassForNonzeroReservedBits() {
        JsonNode decoded = RTCMDecoder.partialDecode(new byte[] {(byte) 0xD3, 0x04, 0, 0, 0, 0});

        assertEquals(1, decoded.size());
        assertEquals("RTCM3", decoded.path("class").asText());
        assertFalse(decoded.has("length"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1013, 1014, 1029, 1033, 1034, 1070, 1071, 1230, 1231})
    void partialDecodeIncludesStationIdOnlyForMessageTypesWithStationId(int type) {
        JsonNode decoded = RTCMDecoder.partialDecode(message(type, 0x201, 0xABC));

        assertEquals("RTCM3", decoded.path("class").asText());
        assertEquals(0x201, decoded.path("length").asInt());
        assertEquals(type, decoded.path("type").asInt());
        if (hasStationId(type)) {
            assertEquals(0xABC, decoded.path("station_id").asInt());
        } else {
            assertFalse(decoded.has("station_id"));
        }
    }

    @Test
    void decodeRtcmUsesPartialDecoderWhenFullDecodeIsDisabled() {
        RTCMDecoder decoder = new RTCMDecoder(false);

        JsonNode decoded = decoder.decodeRtcm("D300133ED980");

        assertEquals("RTCM3", decoded.path("class").asText());
        assertEquals(19, decoded.path("length").asInt());
        assertEquals(1005, decoded.path("type").asInt());
        assertEquals(0x980, decoded.path("station_id").asInt());
    }

    @Test
    void decodeRtcmFallsBackToPartialDecodeWhenExecutableIsUnavailable() {
        assumeFalse(new File("/usr/bin/gpsdecode").exists(),
                "This fallback applies only when the configured gpsdecode executable is absent");
        RTCMDecoder decoder = new RTCMDecoder(true);

        JsonNode decoded = decoder.decodeRtcm("D300133ED980");

        assertEquals(1005, decoded.path("type").asInt());
        assertEquals(0x980, decoded.path("station_id").asInt());
    }

    @Test
    void decodeRtcmRejectsMalformedHex() {
        RTCMDecoder decoder = new RTCMDecoder(false);

        assertThrows(IllegalArgumentException.class, () -> decoder.decodeRtcm("not hex"));
    }

    private static byte[] message(int type, int length, int stationId) {
        return new byte[] {
                (byte) 0xD3,
                (byte) ((length >>> 8) & 0x03),
                (byte) length,
                (byte) (type >>> 4),
                (byte) ((type & 0x0F) << 4 | ((stationId >>> 8) & 0x0F)),
                (byte) stationId
        };
    }

    private static boolean hasStationId(int type) {
        return type <= 1013 || type == 1029 || type == 1033 || (type >= 1071 && type <= 1230);
    }
}
