package us.dot.its.jpo.geojsonconverter.validator;

import org.springframework.stereotype.Service;

/**
 * JSON validator for TIM messages.
 */
@Service
public class TimJsonValidator extends AbstractJsonValidator {

    public TimJsonValidator() {
        super("classpath:schemas/tim.schema.json");
    }

    /**
     * @param schemaLocation The json schema classpath
     */
    public TimJsonValidator(String schemaLocation) {
        super(schemaLocation);
    }

}
