package us.dot.its.jpo.geojsonconverter.converter.tim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;

/** Cuts crossing line edges while preserving source traversal order. */
final class TimLineGeometry {
    private TimLineGeometry() {
    }

    static Geometry toGeoJson(double[][] coordinates) {
        List<double[][]> pieces = new ArrayList<>();
        List<double[]> current = new ArrayList<>();
        current.add(coordinates[0]);
        boolean crossed = false;
        for (int i = 1; i < coordinates.length; i++) {
            double[] previous = current.getLast();
            double[] next = coordinates[i];
            double delta = next[0] - previous[0];
            if (Math.abs(delta) <= 180.0) {
                current.add(next);
                continue;
            }

            crossed = true;
            double unwrappedNext = next[0] - Math.copySign(360.0, delta);
            // +180 and -180 are aliases of the same meridian. Keep a vertical
            // edge on its current side instead of dividing by a zero delta.
            if (unwrappedNext == previous[0]) {
                current.add(new double[] {previous[0], next[1]});
                continue;
            }

            double boundary = delta < 0.0 ? 180.0 : -180.0;
            double fraction = (boundary - previous[0]) / (unwrappedNext - previous[0]);
            double latitude = previous[1] + fraction * (next[1] - previous[1]);
            double[] seam = {boundary, latitude};
            if (!Arrays.equals(previous, seam)) current.add(seam);
            addPiece(pieces, current);
            current = new ArrayList<>();
            current.add(new double[] {-boundary, latitude});
            current.add(next);
        }

        if (!crossed) return new LineString(coordinates);
        addPiece(pieces, current);
        if (pieces.isEmpty()) return null;
        if (pieces.size() == 1) return new LineString(pieces.getFirst());
        return new MultiLineString(pieces.toArray(double[][][]::new));
    }

    private static void addPiece(List<double[][]> pieces, List<double[]> coordinates) {
        if (coordinates.size() < 2) return;
        // A crossing at an endpoint may leave only a repeated boundary point.
        if (coordinates.stream().allMatch(point -> Arrays.equals(point, coordinates.getFirst()))) return;
        pieces.add(coordinates.toArray(double[][]::new));
    }
}
