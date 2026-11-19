package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a lane width profile for a TIM region.
 * <p>
 * This object describes the default lane width set in a TIM along with the calculated actual lane widths (including any
 * offset) for each node represented in the TIM.
 * <p>
 * defaultWidthMeters - The default lane width set in the TIM
 * <p>
 * nodeLaneWidthMeters - List of calculated actual lane widths (including any offset) for each path node. A null entry
 * means that the absolute lane width is unknown for that node.
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedLaneWidthProfile {
    private Double defaultWidthMeters;
    private List<Double> nodeLaneWidthMeters;
}
