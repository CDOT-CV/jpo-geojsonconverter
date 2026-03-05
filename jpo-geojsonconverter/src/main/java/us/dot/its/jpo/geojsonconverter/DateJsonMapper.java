package us.dot.its.jpo.geojsonconverter;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import tools.jackson.databind.ObjectMapper;

public class DateJsonMapper {
    final static ObjectMapper objectMapper;

    static {
        // Initialize the ObjectMapper for general use
        objectMapper = new ObjectMapper();
        objectMapper.setDefaultPropertyInclusion(Include.NON_NULL);

    }

    public static ObjectMapper getInstance() {
        return objectMapper;
    }

}
