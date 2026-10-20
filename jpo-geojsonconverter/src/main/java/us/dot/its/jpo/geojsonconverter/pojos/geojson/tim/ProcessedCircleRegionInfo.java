package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents information about a CIRCLE TIM region.
 * <p>
 * Circle regions include center and radius but do not include lane width profiles and only support heading-based
 * direction info.
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedCircleRegionInfo extends ProcessedRegionInfoBase {
    /** Circle center from description.geometry.circle.center, which is independent of the region anchor. */
    private ProcessedAnchorPoint center;

    /** Circle radius in meters, preserving the precision provided by J2735 DistanceUnits. */
    private Double radius;

    /**
     * Override to ensure only heading-based direction info is allowed for circle regions.
     *
     * @param directionInfo The direction info to set (must be ProcessedHeadingDirectionInfo)
     * @throws IllegalArgumentException if direction info is not heading-based
     */
    @Override
    public void setDirectionInfo(ProcessedDirectionInfoBase directionInfo) {
        if (directionInfo != null && !(directionInfo instanceof ProcessedHeadingDirectionInfo)) {
            throw new IllegalArgumentException("Circle regions only support heading-based direction info");
        }
        super.setDirectionInfo(directionInfo);
    }
}
