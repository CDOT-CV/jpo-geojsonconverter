package us.dot.its.jpo.geojsonconverter.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.ProjCoordinate;

/**
 * Test class for ProjectionUtils to verify coordinate projection and circle generation functionality.
 */
public class ProjectionUtilsTest {

    @Test
    public void testTransformCoordinate() {
        // Test single coordinate transformation
        double originalLon = -104.6636836;
        double originalLat = 41.1501408;

        // Transform to UTM
        ProjCoordinate utmCoord =
                ProjectionUtils.transformCoordinate("EPSG:4326", "EPSG:32613", originalLon, originalLat);
        assertNotNull(utmCoord);

        // Transform back to WGS84
        ProjCoordinate wgs84Coord =
                ProjectionUtils.transformCoordinate("EPSG:32613", "EPSG:4326", utmCoord.x, utmCoord.y);
        assertNotNull(wgs84Coord);

        // Verify the round-trip transformation is reasonably accurate (within 1 meter)
        double tolerance = 0.00001; // Approximately 1 meter
        assertTrue(Math.abs(wgs84Coord.x - originalLon) < tolerance, "Longitude should be preserved within tolerance");
        assertTrue(Math.abs(wgs84Coord.y - originalLat) < tolerance, "Latitude should be preserved within tolerance");
    }

    @Test
    public void testCreateTransform() {
        // Test transform creation
        CoordinateTransform transform = ProjectionUtils.createTransform("EPSG:4326", "EPSG:32613");
        assertNotNull(transform, "Should create a valid coordinate transform");

        // Test invalid CRS codes
        CoordinateTransform invalidTransform = ProjectionUtils.createTransform("INVALID", "EPSG:4326");
        assertTrue(invalidTransform == null, "Should return null for invalid CRS codes");
    }

    @Test
    public void testGetUtmZone() {
        // Test UTM zone calculation
        assertEquals(13, ProjectionUtils.getUtmZone(-104.0)); // Denver area
        assertEquals(1, ProjectionUtils.getUtmZone(-180.0)); // Western edge
        assertEquals(60, ProjectionUtils.getUtmZone(179.0)); // Eastern edge
        assertEquals(31, ProjectionUtils.getUtmZone(0.0)); // Prime meridian
    }

    @Test
    public void testGetUtmCrsCode() {
        // Test UTM CRS code generation
        String northernCrs = ProjectionUtils.getUtmCrsCode(-104.0, 41.0); // Northern hemisphere
        String southernCrs = ProjectionUtils.getUtmCrsCode(-104.0, -41.0); // Southern hemisphere

        assertTrue(northernCrs.startsWith("EPSG:326"), "Northern hemisphere should use 326xx codes");
        assertTrue(southernCrs.startsWith("EPSG:327"), "Southern hemisphere should use 327xx codes");

        // Verify zone numbers are correct
        assertTrue(northernCrs.contains("13"), "Should use correct UTM zone for longitude -104");
        assertTrue(southernCrs.contains("13"), "Should use correct UTM zone for longitude -104");
    }

    @Test
    public void testTransformCoordinates() {
        // Test coordinate transformation from WGS84 to UTM and back
        double originalLon = -104.6636836;
        double originalLat = 41.1501408;

        // Transform to UTM
        double[] utmCoords = ProjectionUtils.transformCoordinates("EPSG:4326", "EPSG:32613", originalLon, originalLat);
        assertNotNull(utmCoords);
        assertEquals(2, utmCoords.length);

        // Transform back to WGS84
        double[] wgs84Coords =
                ProjectionUtils.transformCoordinates("EPSG:32613", "EPSG:4326", utmCoords[0], utmCoords[1]);
        assertNotNull(wgs84Coords);
        assertEquals(2, wgs84Coords.length);

        // Verify the round-trip transformation is reasonably accurate (within 1 meter)
        double tolerance = 0.00001; // Approximately 1 meter
        assertTrue(Math.abs(wgs84Coords[0] - originalLon) < tolerance,
                "Longitude should be preserved within tolerance");
        assertTrue(Math.abs(wgs84Coords[1] - originalLat) < tolerance, "Latitude should be preserved within tolerance");
    }

}
