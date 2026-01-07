package us.dot.its.jpo.geojsonconverter.pojos.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;

/**
 * Represents compliance validation results for a TIM message.
 * <p>
 * standard - The standard being validated against (e.g., "ITWG", "CTW")
 * <p>
 * compliant - Whether the message is compliant with the standard
 * <p>
 * validationMessages - List of validation messages indicating compliance issues
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedTimCompliance {
    private Standard standard;
    private boolean compliant;
    private List<ProcessedValidationMessage> validationMessages;

    // make the standard enum
    public enum Standard {
        ITWG, CTW, J2735
    }
}
