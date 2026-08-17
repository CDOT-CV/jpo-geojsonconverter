package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for TIM region information.
 * <p>
 * regionType - The type of region (PATH, POLYGON, CIRCLE)
 * <p>
 * elevationProfile - Profile of elevations along the region
 * <p>
 * anchorPoint - The anchor point for the region
 * <p>
 * directionInfo - Information about the direction and heading for this region
 */
@Data
@Generated
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "regionType",
        visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = ProcessedPathRegionInfo.class, name = "PATH"),
        @JsonSubTypes.Type(value = ProcessedPolygonRegionInfo.class, name = "POLYGON"),
        @JsonSubTypes.Type(value = ProcessedCircleRegionInfo.class, name = "CIRCLE"),
        @JsonSubTypes.Type(value = ProcessedUnknownRegionInfo.class, name = "UNKNOWN")})
@Slf4j
public abstract class ProcessedRegionInfoBase {
    private ProcessedRegionType regionType;
    private ProcessedElevationProfile elevationProfile;
    private ProcessedAnchorPoint anchorPoint;
    private ProcessedDirectionInfoBase directionInfo;
}
