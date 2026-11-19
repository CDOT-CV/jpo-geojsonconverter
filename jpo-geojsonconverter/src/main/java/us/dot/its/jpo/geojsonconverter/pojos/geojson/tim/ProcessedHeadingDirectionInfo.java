package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents heading-based direction information for a TIM region.
 * <p>
 * This is used when the direction info includes specific heading information with ranges.
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedHeadingDirectionInfo extends ProcessedDirectionInfoBase {
    private List<ProcessedHeading> headingList;
}
