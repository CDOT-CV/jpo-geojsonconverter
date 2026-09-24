package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents information about a POLYGON TIM region.
 * <p>
 * Polygon regions do not include lane width profiles and only support heading-based direction info.
 */
@Data
@Generated
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedPolygonRegionInfo extends ProcessedRegionInfoBase {

    /**
     * Override to ensure only heading-based direction info is allowed for polygon regions.
     * 
     * @param directionInfo The direction info to set (must be ProcessedHeadingDirectionInfo)
     * @throws IllegalArgumentException if direction info is not heading-based
     */
    @Override
    public void setDirectionInfo(ProcessedDirectionInfoBase directionInfo) {
        if (directionInfo != null && !(directionInfo instanceof ProcessedHeadingDirectionInfo)) {
            throw new IllegalArgumentException("Polygon regions only support heading-based direction info");
        }
        super.setDirectionInfo(directionInfo);
    }
}
