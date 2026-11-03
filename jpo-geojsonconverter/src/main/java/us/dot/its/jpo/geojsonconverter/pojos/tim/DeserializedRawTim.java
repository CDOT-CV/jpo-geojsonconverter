package us.dot.its.jpo.geojsonconverter.pojos.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

/**
 * Represents a deserialized raw TIM message with validation results.
 * <p>
 * odeTimMessageFrameData - The deserialized ODE TIM message frame data
 * <p>
 * validatorResults - The validation results from JSON schema validation
 * <p>
 * validationFailure - Whether validation failed
 * <p>
 * failedMessage - The failure message if validation failed
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class DeserializedRawTim {
    private OdeMessageFrameData odeTimMessageFrameData;
    private List<ProcessedValidationMessage> validatorResults;
    private boolean validationFailure = false;
    private String failedMessage;
}
