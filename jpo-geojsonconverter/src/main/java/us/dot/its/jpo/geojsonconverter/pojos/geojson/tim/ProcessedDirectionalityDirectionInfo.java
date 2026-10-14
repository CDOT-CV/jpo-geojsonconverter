package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents directionality-based direction information for a TIM region.
 * <p>
 * This is used when the direction info is based on directionality (forward, reverse, both, unknown) rather than
 * specific heading information.
 */
@Data
@Generated
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedDirectionalityDirectionInfo extends ProcessedDirectionInfoBase {
    private ProcessedDirectionality directionality;
}
