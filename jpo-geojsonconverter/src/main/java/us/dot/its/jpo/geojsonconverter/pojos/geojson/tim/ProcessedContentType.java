package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The selected J2735 TravelerDataFrame content choice, independent of TravelerInfoType.
 */
public enum ProcessedContentType {
    ADVISORY("ADVISORY"), WORK_ZONE("WORK_ZONE"), SPEED_LIMIT("SPEED_LIMIT"),
    GENERIC_SIGN("GENERIC_SIGN"), EXIT_SERVICE("EXIT_SERVICE");

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
