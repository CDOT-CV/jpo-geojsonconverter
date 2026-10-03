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
 * Base class for TIM direction information.
 * <p>
 * type - The type of direction information (directionality, heading)
 * <p>
 * directionality - The directionality (forward, reverse, both, unknown)
 */
@Data
@Generated
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "directionType",
        visible = true)
@JsonSubTypes({@JsonSubTypes.Type(value = ProcessedDirectionalityDirectionInfo.class, name = "DIRECTIONALITY"),
        @JsonSubTypes.Type(value = ProcessedHeadingDirectionInfo.class, name = "HEADING")})
@Slf4j
public abstract class ProcessedDirectionInfoBase {
    private ProcessedDirectionType directionType;
}
