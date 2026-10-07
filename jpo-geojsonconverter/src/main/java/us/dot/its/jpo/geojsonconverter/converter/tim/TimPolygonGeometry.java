package us.dot.its.jpo.geojsonconverter.converter.tim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiPolygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;

/** Converts a continuous polygon into GeoJSON pieces bounded by the antimeridian. */
final class TimPolygonGeometry {
    private TimPolygonGeometry() {
    }

    /** Resolve each edge using neighboring longitudes before planar validity checks. */
    static Coordinate[] unwrapRing(Coordinate[] coordinates) {
        Coordinate[] unwrapped = new Coordinate[coordinates.length];
        unwrapped[0] = new Coordinate(coordinates[0]);
        for (int i = 1; i < coordinates.length; i++) {
            double longitude = coordinates[i].x;
            while (longitude - unwrapped[i - 1].x > 180.0) longitude -= 360.0;
            while (longitude - unwrapped[i - 1].x < -180.0) longitude += 360.0;
            unwrapped[i] = new Coordinate(longitude, coordinates[i].y);
        }

        // A ring around a pole closes geographically after one full longitude turn.
        // Complete its planar shell through that pole before clipping it into bands.
        Coordinate first = unwrapped[0];
        Coordinate last = unwrapped[unwrapped.length - 1];
        if (!first.equals2D(last)) {
            boolean north = Arrays.stream(coordinates).allMatch(point -> point.y > 0.0);
            boolean south = Arrays.stream(coordinates).allMatch(point -> point.y < 0.0);
            if ((!north && !south) || Math.abs(Math.abs(last.x - first.x) - 360.0) > 1e-8) {
                throw new IllegalArgumentException("Polygon has no unambiguous closed longitude frame");
            }
            double pole = north ? 90.0 : -90.0;
            unwrapped = Arrays.copyOf(unwrapped, unwrapped.length + 3);
            unwrapped[unwrapped.length - 3] = new Coordinate(last.x, pole);
            unwrapped[unwrapped.length - 2] = new Coordinate(first.x, pole);
            unwrapped[unwrapped.length - 1] = new Coordinate(first);
        }
        return unwrapped;
    }

    static Geometry toGeoJson(org.locationtech.jts.geom.Polygon polygon) {
        Envelope bounds = polygon.getEnvelopeInternal();
        if (bounds.getMinX() >= -180.0 && bounds.getMaxX() <= 180.0) {
            return new Polygon(coordinates(polygon, false));
        }

        GeometryFactory factory = polygon.getFactory();
        int firstBand = (int) Math.floor((bounds.getMinX() + 180.0) / 360.0);
        int lastBand = (int) Math.floor((bounds.getMaxX() + 180.0) / 360.0);
        List<org.locationtech.jts.geom.Polygon> pieces = new ArrayList<>();
        for (int band = firstBand; band <= lastBand; band++) {
            double shift = band * 360.0;
            var window = factory.toGeometry(new Envelope(-180.0 + shift, 180.0 + shift, -90.0, 90.0));
            var clipped = polygon.intersection(window);
            collectPolygons(clipped, shift, pieces);
        }

        if (pieces.isEmpty()) return null;
        if (pieces.size() == 1) return new Polygon(coordinates(pieces.getFirst(), true));

        // Polar caps can meet along the starting meridian after translation. Merge
        // such shared edges so the result remains a valid polygon or multipolygon.
        var multipart = factory.createMultiPolygon(pieces.toArray(org.locationtech.jts.geom.Polygon[]::new));
        if (!multipart.isValid()) {
            var merged = multipart.union();
            pieces.clear();
            collectPolygons(merged, 0.0, pieces);
            if (pieces.size() == 1) return new Polygon(coordinates(pieces.getFirst(), true));
        }

        double[][][][] result = new double[pieces.size()][][][];
        for (int i = 0; i < pieces.size(); i++) result[i] = coordinates(pieces.get(i), true);
        return new MultiPolygon(result);
    }

    private static void collectPolygons(org.locationtech.jts.geom.Geometry geometry, double shift,
            List<org.locationtech.jts.geom.Polygon> pieces) {
        if (geometry instanceof org.locationtech.jts.geom.Polygon polygon) {
            if (polygon.isEmpty() || polygon.getArea() <= 0.0) return;
            var translated = (org.locationtech.jts.geom.Polygon) polygon.copy();
            translated.apply((org.locationtech.jts.geom.CoordinateFilter) coordinate ->
                    coordinate.x = Math.clamp(coordinate.x - shift, -180.0, 180.0));
            translated.geometryChanged();
            pieces.add(translated);
        } else if (geometry instanceof org.locationtech.jts.geom.GeometryCollection collection) {
            for (int i = 0; i < collection.getNumGeometries(); i++) {
                collectPolygons(collection.getGeometryN(i), shift, pieces);
            }
        }
        // Intersections that only touch a band boundary may be points or lines.
    }

    private static double[][][] coordinates(org.locationtech.jts.geom.Polygon polygon, boolean orientRings) {
        double[][][] result = new double[polygon.getNumInteriorRing() + 1][][];
        for (int ringIndex = 0; ringIndex < result.length; ringIndex++) {
            var ring = ringIndex == 0 ? polygon.getExteriorRing() : polygon.getInteriorRingN(ringIndex - 1);
            Coordinate[] points = ring.getCoordinates();
            if (orientRings && Orientation.isCCW(points) != (ringIndex == 0)) CoordinateArrays.reverse(points);
            result[ringIndex] = new double[points.length][2];
            for (int i = 0; i < points.length; i++) {
                result[ringIndex][i][0] = points[i].x;
                result[ringIndex][i][1] = points[i].y;
            }
        }
        return result;
    }
}
