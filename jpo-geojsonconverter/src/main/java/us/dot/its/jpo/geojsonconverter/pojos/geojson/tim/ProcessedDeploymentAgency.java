package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInfoType;

/**
 * Enum for deployment agency types in TIM messages.
 */
public enum ProcessedDeploymentAgency {
    STATE_OR_LOCAL("STATE_OR_LOCAL"), COMMERCIAL("COMMERCIAL"), UNKNOWN("UNKNOWN");

    private final String value;

    ProcessedDeploymentAgency(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ProcessedDeploymentAgency fromValue(String value) {
        for (ProcessedDeploymentAgency agency : values()) {
            if (agency.value.equals(value)) {
                return agency;
            }
        }
        throw new IllegalArgumentException("Unknown deployment agency: " + value);
    }

    // roadSignage = STATE_OR_LOCAL_AGENCY
    // commercialSignage = COMMERCIAL_AGENCY
    public static ProcessedDeploymentAgency fromValue(TravelerInfoType value) {
        if (value == TravelerInfoType.ROADSIGNAGE) {
            return ProcessedDeploymentAgency.STATE_OR_LOCAL;
        } else if (value == TravelerInfoType.COMMERCIALSIGNAGE) {
            return ProcessedDeploymentAgency.COMMERCIAL;
        } else {
            return ProcessedDeploymentAgency.UNKNOWN;
        }
    }
}
