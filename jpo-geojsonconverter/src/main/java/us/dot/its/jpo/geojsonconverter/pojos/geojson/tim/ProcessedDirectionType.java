package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for direction types in TIM messages.
 */
public enum ProcessedDirectionType {
    DIRECTIONALITY("DIRECTIONALITY"), HEADING("HEADING");

    private final String value;

    ProcessedDirectionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProcessedDirectionType fromValue(String value) {
        for (ProcessedDirectionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown direction type: " + value);
    }
}
