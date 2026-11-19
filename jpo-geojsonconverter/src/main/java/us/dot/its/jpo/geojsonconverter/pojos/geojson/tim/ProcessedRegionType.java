package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for region types in TIM messages.
 */
public enum ProcessedRegionType {
    PATH("PATH"), POLYGON("POLYGON"), CIRCLE("CIRCLE"), UNKNOWN("UNKNOWN");

    private final String value;

    ProcessedRegionType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProcessedRegionType fromValue(String value) {
        for (ProcessedRegionType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown region type: " + value);
    }
}
