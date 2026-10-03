package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeometryValueSemanticsTest {

    @Test
    void multiLineStringPreservesCoordinatesAndValueEquality() {
        double[][][] coordinates = {{{-105.0, 40.0}, {-104.9, 40.1}}, {{-104.8, 40.2}, {-104.7, 40.3}}};
        MultiLineString geometry = new MultiLineString(coordinates);
        MultiLineString equalGeometry = new MultiLineString(new double[][][] {
                {{-105.0, 40.0}, {-104.9, 40.1}}, {{-104.8, 40.2}, {-104.7, 40.3}}});

        assertEquals("MultiLineString", geometry.getGeoJSONType());
        assertArrayEquals(coordinates, geometry.getCoordinates());
        assertNull(geometry.getBbox());
        assertEquals(geometry, geometry);
        assertEquals(geometry, equalGeometry);
        assertEquals(geometry.hashCode(), equalGeometry.hashCode());
        assertTrue(geometry.toString().contains("coordinates"));
        assertNotEquals(geometry, new MultiLineString(new double[][][] {{{-105.0, 40.0}}}));
        assertNotEquals(geometry, new MultiLineString(null));
        assertNotEquals(null, geometry);
        assertNotEquals(geometry, new Object());
        assertEquals(new MultiLineString(null), new MultiLineString(null));
    }

    @Test
    void polygonPreservesCoordinatesAndValueEquality() {
        double[][][] coordinates = {{{-105.0, 40.0}, {-104.9, 40.1}, {-104.8, 40.0}, {-105.0, 40.0}}};
        Polygon geometry = new Polygon(coordinates);
        Polygon equalGeometry = new Polygon(new double[][][] {
                {{-105.0, 40.0}, {-104.9, 40.1}, {-104.8, 40.0}, {-105.0, 40.0}}});

        assertEquals("Polygon", geometry.getGeoJSONType());
        assertArrayEquals(coordinates, geometry.getCoordinates());
        assertNull(geometry.getBbox());
        assertEquals(geometry, geometry);
        assertEquals(geometry, equalGeometry);
        assertEquals(geometry.hashCode(), equalGeometry.hashCode());
        assertTrue(geometry.toString().contains("coordinates"));
        assertNotEquals(geometry, new Polygon(new double[][][] {{{-105.0, 40.0}, {-105.0, 40.0}}}));
        assertNotEquals(geometry, new Polygon(null));
        assertNotEquals(null, geometry);
        assertNotEquals(geometry, new Object());
        assertEquals(new Polygon(null), new Polygon(null));
    }

    @Test
    void multiPolygonPreservesCoordinatesAndValueEquality() {
        double[][][][] coordinates = {{{{-105.0, 40.0}, {-104.9, 40.1}, {-104.8, 40.0}, {-105.0, 40.0}}}};
        MultiPolygon geometry = new MultiPolygon(coordinates);
        MultiPolygon equalGeometry = new MultiPolygon(new double[][][][] {
                {{{-105.0, 40.0}, {-104.9, 40.1}, {-104.8, 40.0}, {-105.0, 40.0}}}});

        assertEquals("MultiPolygon", geometry.getGeoJSONType());
        assertArrayEquals(coordinates, geometry.getCoordinates());
        assertNull(geometry.getBbox());
        assertEquals(geometry, geometry);
        assertEquals(geometry, equalGeometry);
        assertEquals(geometry.hashCode(), equalGeometry.hashCode());
        assertTrue(geometry.toString().contains("coordinates"));
        assertNotEquals(geometry, new MultiPolygon(new double[][][][] {{{{-105.0, 40.0}}}}));
        assertNotEquals(geometry, new MultiPolygon(null));
        assertNotEquals(null, geometry);
        assertNotEquals(geometry, new Object());
        assertEquals(new MultiPolygon(null), new MultiPolygon(null));
    }
}
