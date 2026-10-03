package us.dot.its.jpo.geojsonconverter.converter.tim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;

/**
 * Geometry produced for one TIM data frame together with the component index assigned to each source region.
 *
 * <p>A mapping entry is {@code null} when the corresponding source region could not be converted. For a simple
 * geometry the converted region maps to index {@code 0}; for multi-geometries and geometry collections the index
 * addresses the corresponding coordinate or geometry component.</p>
 */
record TimGeometryResult(Geometry geometry, List<Integer> regionGeometryIndices) {
    TimGeometryResult {
        regionGeometryIndices = Collections.unmodifiableList(new ArrayList<>(regionGeometryIndices));
    }
}
