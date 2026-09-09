package us.dot.its.jpo.geojsonconverter.pojos.common;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;

/**
 * Security metadata supplied by an IEEE 1609.2 signed message.
 *
 * <p>This message-type-independent representation can be attached to processed
 * TIM, BSM, MAP, SPaT, and other J2735 message outputs.
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ieee1609Dot2SignedDataMetadata {
    private Integer psid;
    private Instant generationTime;
    private Instant certificateValidityStart;
    private Instant certificateValidityEnd;
}
