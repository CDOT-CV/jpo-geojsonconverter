package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the properties of a TIM region feature.
 * <p>
 * deploymentAgencyType - The agency responsible for deploying the TIM message
 * <p>
 * validityPeriod - The validity period for the TIM message
 * <p>
 * priority - The priority level of the TIM message
 * <p>
 * regionInfoList - List of information about each region geometry and characteristics
 * <p>
 * content - The content of the TIM message including ITIS codes and text
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"deploymentAgencyType", "validityPeriod", "priority", "regionInfoList", "content"})
@Slf4j
public class ProcessedTimProperties {
    private ProcessedDeploymentAgency deploymentAgencyType;
    private ProcessedValidityPeriod validityPeriod;
    private Integer priority;
    private List<ProcessedRegionInfoBase> regionInfoList;
    private ProcessedTimContent content;
}
