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
 * Represents an elevation profile for a TIM region.
 * <p>
 * defaultElevationMeters - The default elevation
 * <p>
 * nodeElevationMeters - List of calculated elevations for each path node. A null entry means that the absolute
 * elevation is unknown for that node.
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedElevationProfile {
    private Double defaultElevationMeters;
    private List<Double> nodeElevationMeters;
}
