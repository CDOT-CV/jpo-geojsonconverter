package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.BaseFeatureCollection;

@JsonPropertyOrder({"schemaVersion", "features", "messageType", "odeReceivedAt", "timeStamp", "originIp", "validationMessages"})
public class ProcessedTim<Point> extends BaseFeatureCollection<TimFeature<Point>> {
    private static Logger logger = LoggerFactory.getLogger(ProcessedTim.class);

    // Default schemaVersion is -1 for older messages that lack a schemaVersion value
    private int schemaVersion = -1;
    private String messageType = "TIM";
    private String odeReceivedAt;
    private String originIp;
    private String logName;
    private List<ProcessedValidationMessage> validationMessages = null;
    private ZonedDateTime timeStamp;

    @JsonCreator
    public ProcessedTim(@JsonProperty("features") TimFeature<Point>[] features) {
        super(features);
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return this.messageType;
    }

    public String getOdeReceivedAt() {
        return odeReceivedAt;
    }

    public void setOdeReceivedAt(String odeReceivedAt) {
        this.odeReceivedAt = odeReceivedAt;
    }

    public void setOriginIp(String originIp) {
        this.originIp = originIp;
    }

    public String getOriginIp() {
        return this.originIp;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public String getLogName() {
        return this.logName;
    }

    public List<ProcessedValidationMessage> getValidationMessages() {
        return validationMessages;
    }

    public void setValidationMessages(List<ProcessedValidationMessage> validationMessages) {
        this.validationMessages = validationMessages;
    }

    public void setTimeStamp(ZonedDateTime timeStamp) {
        this.timeStamp = timeStamp;
    }

    public ZonedDateTime getTimeStamp() {
        return this.timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ProcessedTim)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        ProcessedTim<Point> processedTim = (ProcessedTim<Point>) o;
        return (
            Objects.equals(getFeatures(), processedTim.getFeatures()) && 
            Objects.equals(getSchemaVersion(), processedTim.getSchemaVersion()) && 
            Objects.equals(getMessageType(), processedTim.getMessageType()) && 
            Objects.equals(getOdeReceivedAt(), processedTim.getOdeReceivedAt()) && 
            Objects.equals(getOriginIp(), processedTim.getOriginIp()) && 
            Objects.equals(getLogName(), processedTim.getLogName()) && 
            Objects.equals(getValidationMessages(), processedTim.getValidationMessages()) && 
            Objects.equals(getTimeStamp(), processedTim.getTimeStamp()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageType, odeReceivedAt, originIp, logName, validationMessages, timeStamp, getFeatures());
    }

    @Override
    public String toString() {
        ObjectMapper mapper = DateJsonMapper.getInstance();
        String testReturn = "";
        try {
            testReturn = (mapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }
        return testReturn;
    }
}
