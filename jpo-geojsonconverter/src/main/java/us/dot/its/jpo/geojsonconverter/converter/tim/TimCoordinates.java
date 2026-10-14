package us.dot.its.jpo.geojsonconverter.converter.tim;

/**
 * Coordinate precision for TIM geometry. Accumulating node offsets and projecting across the antimeridian introduce
 * double rounding error on the order of 1e-14 degrees, enough to make a ring's returning node differ from its first
 * node. Eight decimal places (about 1 mm) is finer than any J2735 position resolution (1e-7 degrees) and removes that
 * noise before positions are compared or serialized.
 */
final class TimCoordinates {
    private static final double SCALE = 1e8;

    private TimCoordinates() {
    }

    static double round(double degrees) {
        if (!Double.isFinite(degrees)) return degrees;
        return Math.round(degrees * SCALE) / SCALE;
    }

    static double[][] round(double[][] positions) {
        double[][] rounded = new double[positions.length][];
        for (int i = 0; i < positions.length; i++) {
            rounded[i] = new double[] {round(positions[i][0]), round(positions[i][1])};
        }
        return rounded;
    }
}
