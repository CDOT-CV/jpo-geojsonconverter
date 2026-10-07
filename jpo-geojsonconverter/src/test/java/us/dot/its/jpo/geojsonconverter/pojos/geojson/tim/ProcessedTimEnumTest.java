package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInfoType;

class ProcessedTimEnumTest {

    @Test
    void deploymentAgencyMapsProcessedAndAsnValues() {
        for (ProcessedDeploymentAgency agency : ProcessedDeploymentAgency.values()) {
            assertEquals(agency, ProcessedDeploymentAgency.fromValue(agency.getValue()));
        }
        assertEquals(ProcessedDeploymentAgency.STATE_OR_LOCAL,
                ProcessedDeploymentAgency.fromValue(TravelerInfoType.ROADSIGNAGE));
        assertEquals(ProcessedDeploymentAgency.COMMERCIAL,
                ProcessedDeploymentAgency.fromValue(TravelerInfoType.COMMERCIALSIGNAGE));
        TravelerInfoType otherType = Arrays.stream(TravelerInfoType.values())
                .filter(type -> type != TravelerInfoType.ROADSIGNAGE && type != TravelerInfoType.COMMERCIALSIGNAGE)
                .findFirst().orElseThrow();
        assertEquals(ProcessedDeploymentAgency.UNKNOWN, ProcessedDeploymentAgency.fromValue(otherType));
        assertThrows(IllegalArgumentException.class, () -> ProcessedDeploymentAgency.fromValue("unknown-value"));
    }

    @Test
    void directionalityMapsEveryWireValueAndRejectsUnknownValues() {
        for (ProcessedDirectionality directionality : ProcessedDirectionality.values()) {
            assertEquals(directionality, ProcessedDirectionality.fromValue(directionality.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> ProcessedDirectionality.fromValue("sideways"));
    }

    @Test
    void contentTypeMapsEveryWireValueAndRejectsUnknownValues() {
        for (ProcessedContentType contentType : ProcessedContentType.values()) {
            assertEquals(contentType, ProcessedContentType.fromValue(contentType.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> ProcessedContentType.fromValue("unrecognized"));
    }
}
