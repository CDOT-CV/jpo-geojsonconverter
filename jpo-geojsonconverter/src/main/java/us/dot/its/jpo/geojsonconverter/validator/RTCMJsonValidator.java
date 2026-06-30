package us.dot.its.jpo.geojsonconverter.validator;

import org.springframework.stereotype.Service;

/**
 * JSON validator for RTCM messages.
 * Validates J2735 requirements and ODE metadata.
 */
@Service
public class RTCMJsonValidator extends AbstractJsonValidator {

    public RTCMJsonValidator() {
        super("classpath:schemas/rtcm.schema.json");
    }

    /**
     * @param schemaLocation The json schema classpath
     */
    public RTCMJsonValidator(String schemaLocation) {
        super(schemaLocation);
    }
}
