package us.dot.its.jpo.geojsonconverter.pojos.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.common.Ieee1609Dot2SignedDataMetadata;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedTimFeatureCollection;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;

/**
 * Represents a processed TIM (Traveler Information Message) message.
 * <p>
 * schemaVersion - The jpo-geojsonconverter schema version for ProcessedTim
 * <p>
 * messageType - TIM
 * <p>
 * odeReceivedAt - The time the origin OdeTimJson message was received by the ODE, in UTC
 * <p>
 * originIp - The IP address the origin OdeTimJson message was received from
 * <p>
 * asn1 - The ASN.1 encoded string of the origin J2735 TIM message
 * <p>
 * signedDataMetadata - IEEE 1609.2 signed-message metadata, when supplied by the ODE
 * <p>
 * isCertPresent - Whether the signing certificate was included with the incoming message
 * <p>
 * msgCnt - The message count of the TIM
 * <p>
 * timeStamp - The timestamp of the TIM message in UTC
 * <p>
 * packetId - The packet ID of the TIM message
 * <p>
 * location - GeoJSON Point representing the center location for MongoDB 2D sphere indexing
 * <p>
 * validationMessages - J2735/ODE JSON schema validation messages
 * <p>
 * dataFrameFeatureCollection - GeoJSON FeatureCollection containing the TIM data frames
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedTim {
    private int schemaVersion = 1;
    private final String messageType = "TIM";
    private String odeReceivedAt;
    private String originIp;
    private String asn1;
    private Ieee1609Dot2SignedDataMetadata signedDataMetadata;
    private boolean certPresent;
    private Integer msgCnt;
    private ZonedDateTime timeStamp;
    private String packetId;
    private Point location;
    private List<ProcessedValidationMessage> validationMessages = new ArrayList<>();
    private ProcessedTimFeatureCollection dataFrameFeatureCollection;

    public void addValidationMessage(ProcessedValidationMessage message) {
        if (validationMessages == null) {
            validationMessages = new ArrayList<ProcessedValidationMessage>();
        }
        validationMessages.add(message);
    }

    public void addValidationMessages(List<ProcessedValidationMessage> messages) {
        if (validationMessages == null) {
            validationMessages = new ArrayList<>();
        }
        validationMessages.addAll(messages);
    }

    public void addValidationMessage(String message) {
        var validationMessage = new ProcessedValidationMessage();
        validationMessage.setMessage(message);
        addValidationMessage(validationMessage);
    }

    @JsonProperty("isCertPresent")
    public boolean isCertPresent() {
        return certPresent;
    }

    @JsonProperty("isCertPresent")
    public void setCertPresent(boolean certPresent) {
        this.certPresent = certPresent;
    }

    @Override
    public String toString() {
        ObjectMapper mapper = DateJsonMapper.getInstance();
        String testReturn = "";
        try {
            testReturn = (mapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return testReturn;
    }
}
