package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for content types in TIM messages.
 */
public enum ProcessedContentType {
    ADVISORY("ADVISORY"), ROAD_SIGNAGE("ROAD_SIGNAGE"), COMMERCIAL_SIGNAGE("COMMERCIAL_SIGNAGE");

    private final String value;

    ProcessedContentType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProcessedContentType fromValue(String value) {
        for (ProcessedContentType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown content type: " + value);
    }
}
