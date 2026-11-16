package us.dot.its.jpo.geojsonconverter.pojos.common;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import us.dot.its.jpo.ode.model.SignedDataMetadata;

/**
 * Security metadata supplied by an IEEE 1609.2 signed message.
 *
 * <p>This message-type-independent representation can be attached to processed
 * TIM, BSM, MAP, SPaT, and other J2735 message outputs. It is populated from the
 * ODE {@code metadata.certMetadata}. Signature {@code generationTime} and
 * {@code expiryTime} describe this signed message. Certificate validity describes
 * the signing certificate. Neither replaces a J2735 timestamp or a TIM
 * applicability period.
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Ieee1609Dot2SignedDataMetadata {
    private Long psid;
    private Instant generationTime;
    private Instant expiryTime;
    private Instant certificateValidityStart;
    private Instant certificateValidityEnd;

    /**
     * Maps the ODE {@code metadata.certMetadata} model to the processed-message representation.
     *
     * @param certMetadata ODE signed-data metadata, may be {@code null}
     * @return processed signed-data metadata, or {@code null} when the ODE frame provides none
     */
    public static Ieee1609Dot2SignedDataMetadata fromOde(SignedDataMetadata certMetadata) {
        if (certMetadata == null) {
            return null;
        }
        return new Ieee1609Dot2SignedDataMetadata(certMetadata.getPsid(), certMetadata.getGenerationTime(),
                certMetadata.getExpiryTime(), certMetadata.getCertificateValidityStart(),
                certMetadata.getCertificateValidityEnd());
    }
}
