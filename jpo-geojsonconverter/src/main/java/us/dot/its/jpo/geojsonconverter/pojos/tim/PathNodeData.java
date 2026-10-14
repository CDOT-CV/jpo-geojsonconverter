package us.dot.its.jpo.geojsonconverter.pojos.tim;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data structure to hold coordinate and offset information for TIM path processing.
 */
@Data
@AllArgsConstructor
public class PathNodeData {
    private final List<Double> coordinates;
    private final Long dwidthOffset;
    private final Long delevationOffset;
}
