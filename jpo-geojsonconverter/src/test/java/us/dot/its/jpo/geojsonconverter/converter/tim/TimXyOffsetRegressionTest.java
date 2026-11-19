package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;

class TimXyOffsetRegressionTest {
    @ParameterizedTest
    @CsvSource({"1, 0", "1, 15", "1, 2", "2, 2", "3, 2", "4, 2", "5, 2", "6, 2",
            "1, -1", "2, -1", "3, -1", "4, -1", "5, -1", "6, -1"})
    void xyVariantsApplyZoomAndAccumulateSignedOffsets(int variant, int zoom) throws Exception {
        String json = """
                {
                  "anchor": {"lat": 0, "long": 0},
                  "description": {"path": {
                    "scale": %d,
                    "offset": {"xy": {"nodes": [
                      {"delta": {"node-XY%d": {"x": 100, "y": 0}}},
                      {"delta": {"node-XY%d": {"x": -50, "y": 0}}}
                    ]}}
                  }}
                }
                """.formatted(zoom, variant, variant);
        var region = DateJsonMapper.getInstance().readValue(json, GeographicalPath.class);

        var line = assertInstanceOf(LineString.class, new TimGeometryConverter().createGeometryFromRegion(region));

        // At the equator WGS84's east-west radius is exactly 6,378,137 m. One metre east followed by half a
        // metre west must retain the running reference and apply 2^zoom to both signed centimetre offsets.
        double degreesPerMeter = 180.0 / (Math.PI * 6_378_137.0);
        double scale = Math.pow(2.0, zoom);
        assertEquals(2, line.getCoordinates().length);
        assertEquals(degreesPerMeter * scale, line.getCoordinates()[0][0], 5e-9);
        assertEquals(degreesPerMeter * scale / 2.0, line.getCoordinates()[1][0], 5e-9);
        assertEquals(0.0, line.getCoordinates()[0][1], 5e-9);
        assertEquals(0.0, line.getCoordinates()[1][1], 5e-9);
    }
}
