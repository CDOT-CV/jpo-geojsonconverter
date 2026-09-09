package us.dot.its.jpo.geojsonconverter.validator;

import org.springframework.stereotype.Service;


/**
 * JSON validator for MapData messages.
 */
@Service
public class MapJsonValidator extends AbstractJsonValidator {

    public MapJsonValidator() {
        super("classpath:schemas/map.schema.json");
    }

    /**
     * @param schemaLocation The json schema classpath
     */
    public MapJsonValidator(String schemaLocation) {
        super(schemaLocation);
    }


}
