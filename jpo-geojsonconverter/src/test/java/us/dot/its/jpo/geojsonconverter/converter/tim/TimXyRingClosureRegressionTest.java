package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.runtime.types.Asn1Boolean;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;

/**
 * Closed XY paths whose offsets return exactly to the first node still end slightly off it in degrees, because each
 * offset is applied as a geodesic step from the previous node. Even after rounding to 8 decimals, the closing node
 * can land 1e-8 degrees from the first, which used to produce an extra, nearly duplicate closing vertex.
 */
class TimXyRingClosureRegressionTest {

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            // Triangle: closing node rounded to 33.74909015 against a first node of 33.74909016.
            "triangle | 337490000 | -843880000 | 0 | 0,1000; 0,6000; 6000,0; -6000,-6000 | 4",
            // Rectangle with scale 3: closing node rounded to -121.49439999 / 38.58169007.
            "rectangle scale 3 | 385816000 | -1214944000 | 3 | 0,125; 0,1000; 1500,0; 0,-1000; -1500,0 | 5",
            // Square: closing node rounded to 39.73929006 against a first node of 39.73929007.
            "square | 397392000 | -1049903000 | 0 | 0,1000; 0,5000; 5000,0; 0,-5000; -5000,0 | 5"})
    void closedXyPathReturningToFirstNodeClosesWithoutNearDuplicateVertex(String name, long anchorLat,
            long anchorLong, int scale, String offsets, int expectedPositions) throws Exception {
        StringBuilder nodes = new StringBuilder();
        for (String offset : offsets.split(";")) {
            String[] xy = offset.trim().split(",");
            if (!nodes.isEmpty()) {
                nodes.append(",");
            }
            nodes.append("{\"delta\": {\"node-XY6\": {\"x\": %s, \"y\": %s}}}".formatted(xy[0], xy[1]));
        }
        String json = """
                {
                  "anchor": {"lat": %d, "long": %d},
                  "description": {"path": {"scale": %d, "offset": {"xy": {"nodes": [%s]}}}}
                }
                """.formatted(anchorLat, anchorLong, scale, nodes);
        GeographicalPath region = DateJsonMapper.getInstance().readValue(json, GeographicalPath.class);
        region.setClosedPath(new Asn1Boolean(true));

        Polygon polygon = assertInstanceOf(Polygon.class, new TimGeometryConverter().createGeometryFromRegion(region));

        double[][] ring = polygon.getCoordinates()[0];
        assertEquals(expectedPositions, ring.length, "one position per distinct node plus the closing position");
        assertArrayEquals(ring[0], ring[ring.length - 1], "ring closes exactly on its first position");
        for (int i = 0; i < ring.length - 1; i++) {
            boolean nearDuplicate = Math.abs(ring[i][0] - ring[i + 1][0]) <= 1e-6
                    && Math.abs(ring[i][1] - ring[i + 1][1]) <= 1e-6;
            assertFalse(nearDuplicate, "positions " + i + " and " + (i + 1) + " must not be near duplicates");
        }
    }
}
