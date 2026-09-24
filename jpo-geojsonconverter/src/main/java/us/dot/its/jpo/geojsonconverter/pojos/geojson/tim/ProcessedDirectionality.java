package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum for directionality values in TIM messages.
 */
public enum ProcessedDirectionality {
    FORWARD("forward"), REVERSE("reverse"), BOTH("both"), UNAVAILABLE("unavailable"), UNKNOWN("unknown");

    private final String value;

    ProcessedDirectionality(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProcessedDirectionality fromValue(String value) {
        for (ProcessedDirectionality directionality : values()) {
            if (directionality.value.equals(value)) {
                return directionality;
            }
        }
        throw new IllegalArgumentException("Unknown directionality: " + value);
    }
}
