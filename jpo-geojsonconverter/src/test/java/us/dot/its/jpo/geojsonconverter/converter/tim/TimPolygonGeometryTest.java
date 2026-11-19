package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiPolygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;

class TimPolygonGeometryTest {
    private final GeometryFactory factory = new GeometryFactory();

    @Test
    void ambiguousFullTurnRingAcrossBothHemispheresIsRejected() {
        Coordinate[] ring = coordinates(new double[][] {
                {-135, 20}, {-45, 20}, {45, -20}, {135, -20}, {-135, 20}
        });

        assertThrows(IllegalArgumentException.class, () -> TimPolygonGeometry.unwrapRing(ring));
    }

    @Test
    void polarRingGetsAPlanarClosureThroughThePole() {
        Coordinate[] unwrapped = TimPolygonGeometry.unwrapRing(coordinates(new double[][] {
                {0, 80}, {90, 80}, {180, 80}, {-90, 80}, {0, 80}
        }));

        assertEquals(8, unwrapped.length);
        assertEquals(360.0, unwrapped[4].x);
        assertEquals(90.0, unwrapped[5].y);
        assertEquals(360.0, unwrapped[5].x);
        assertEquals(90.0, unwrapped[6].y);
        assertEquals(0.0, unwrapped[6].x);
        assertTrue(unwrapped[0].equals2D(unwrapped[7]));
    }

    @Test
    void seamCrossingHoleAndInteriorHoleKeepAreaAndGeoJsonRingRules() {
        var source = polygon(
                new double[][] {{179, 10}, {181, 10}, {181, 14}, {179, 14}, {179, 10}},
                new double[][] {{179.7, 11}, {180.3, 11}, {180.3, 13}, {179.7, 13}, {179.7, 11}},
                new double[][] {{179.1, 11.3}, {179.3, 11.3}, {179.3, 11.7}, {179.1, 11.7}, {179.1, 11.3}});
        assertTrue(source.isValid());
        assertEquals(6.72, source.getArea(), 1e-12);

        MultiPolygon result = assertInstanceOf(MultiPolygon.class, TimPolygonGeometry.toGeoJson(source));
        assertEquals(2, result.getCoordinates().length);

        var actual = toJts(result);
        assertTrue(actual.isValid());
        assertEquals(6.72, actual.getArea(), 1e-12);
        assertEquals(1, Arrays.stream(result.getCoordinates()).mapToInt(polygon -> polygon.length - 1).sum());
        assertTrue(actual.contains(factory.createPoint(new Coordinate(179.2, 13.5))));
        assertTrue(actual.contains(factory.createPoint(new Coordinate(-179.8, 13.5))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(179.2, 11.5))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(179.85, 12))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(-179.85, 12))));
        assertBoundedClosedAndOriented(result);
    }

    @Test
    void concaveClipCollectsDisconnectedPolygonsAcrossTheDateline() {
        var source = polygon(new double[][] {
                {179, 0}, {181, 0}, {181, 4}, {179, 4}, {179, 3},
                {180.5, 3}, {180.5, 1}, {179, 1}, {179, 0}
        });
        assertTrue(source.isValid());
        assertEquals(5.0, source.getArea(), 1e-12);

        MultiPolygon result = assertInstanceOf(MultiPolygon.class, TimPolygonGeometry.toGeoJson(source));
        assertEquals(3, result.getCoordinates().length);

        var actual = toJts(result);
        assertTrue(actual.isValid());
        assertEquals(5.0, actual.getArea(), 1e-12);
        double[] componentAreas = Arrays.stream(result.getCoordinates())
                .mapToDouble(this::toJtsPolygonArea)
                .sorted()
                .toArray();
        assertArrayEquals(new double[] {1.0, 1.0, 3.0}, componentAreas, 1e-12);
        assertTrue(actual.contains(factory.createPoint(new Coordinate(179.5, 0.5))));
        assertTrue(actual.contains(factory.createPoint(new Coordinate(179.5, 3.5))));
        assertTrue(actual.contains(factory.createPoint(new Coordinate(-179.25, 2))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(179.5, 2))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(0, 2))));
        assertBoundedClosedAndOriented(result);
    }

    private Coordinate[] coordinates(double[][] values) {
        return Arrays.stream(values).map(point -> new Coordinate(point[0], point[1])).toArray(Coordinate[]::new);
    }

    private org.locationtech.jts.geom.Polygon polygon(double[][] shell, double[][]... holes) {
        LinearRing[] holeRings = Arrays.stream(holes).map(this::ring).toArray(LinearRing[]::new);
        return factory.createPolygon(ring(shell), holeRings);
    }

    private LinearRing ring(double[][] values) {
        return factory.createLinearRing(coordinates(values));
    }

    private org.locationtech.jts.geom.Geometry toJts(Geometry geometry) {
        if (geometry instanceof Polygon polygon) return toJtsPolygon(polygon.getCoordinates());
        MultiPolygon multiPolygon = assertInstanceOf(MultiPolygon.class, geometry);
        return factory.createMultiPolygon(Arrays.stream(multiPolygon.getCoordinates())
                .map(this::toJtsPolygon)
                .toArray(org.locationtech.jts.geom.Polygon[]::new));
    }

    private org.locationtech.jts.geom.Polygon toJtsPolygon(double[][][] coordinates) {
        LinearRing[] holes = Arrays.stream(coordinates).skip(1).map(this::ring).toArray(LinearRing[]::new);
        return factory.createPolygon(ring(coordinates[0]), holes);
    }

    private double toJtsPolygonArea(double[][][] coordinates) {
        return toJtsPolygon(coordinates).getArea();
    }

    private void assertBoundedClosedAndOriented(MultiPolygon geometry) {
        for (double[][][] polygon : geometry.getCoordinates()) {
            for (int ringIndex = 0; ringIndex < polygon.length; ringIndex++) {
                double[][] ring = polygon[ringIndex];
                assertArrayEquals(ring[0], ring[ring.length - 1], 0.0);
                assertEquals(ringIndex == 0, Orientation.isCCW(coordinates(ring)));
                for (double[] point : ring) {
                    assertTrue(point[0] >= -180 && point[0] <= 180);
                    assertTrue(point[1] >= -90 && point[1] <= 90);
                }
            }
        }
    }
}
