package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the validity period for a TIM message.
 * <p>
 * startTime - The start time of the validity period
 * <p>
 * endTime - The end time of the validity period (null if infinite)
 * <p>
 * isInfinite - Whether the validity period is infinite
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedValidityPeriod {
    private ZonedDateTime startTime;
    private ZonedDateTime endTime;
    private boolean isInfinite;
}
