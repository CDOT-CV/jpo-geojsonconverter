package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import java.util.Objects;

import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeTimData;

public class DeserializedRawTim {
    
    OdeTimData odeTimData;
    JsonValidatorResult validatorResults;
    Boolean validationFailure = false;
    String failedMessage = null;

    public OdeTimData getOdeTimData() {
        return this.odeTimData;
    }

    public void setOdeTimData(OdeTimData odeTimData) {
        this.odeTimData = odeTimData;
    }

    public JsonValidatorResult getValidatorResults() {
        return this.validatorResults;
    }

    public void setValidatorResults(JsonValidatorResult validatorResults) {
        this.validatorResults = validatorResults;
    }

    public Boolean getValidationFailure() {
        return this.validationFailure;
    }

    public void setValidationFailure(Boolean validationFailure) {
        this.validationFailure = validationFailure;
    }

    public String getFailedMessage() {
        return this.failedMessage;
    }

    public void setFailedMessage(String failedMessage) {
        this.failedMessage = failedMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DeserializedRawTim)) {
            return false;
        }
        DeserializedRawTim deserializedRawTim = (DeserializedRawTim) o;
        return Objects.equals(odeTimData, deserializedRawTim.odeTimData) && Objects.equals(validatorResults, deserializedRawTim.validatorResults) && Objects.equals(validationFailure, deserializedRawTim.validationFailure) && Objects.equals(failedMessage, deserializedRawTim.failedMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(odeTimData, validatorResults, validationFailure, failedMessage);
    }

    @Override
    public String toString() {
        return "{" +
            " odeSpatOdeSpat='" + getOdeTimData() + "'" +
            ", validatorResults='" + getValidatorResults() + "'" +
            ", validationFailure='" + getValidationFailure() + "'" +
            ", failedMessage='" + getFailedMessage() + "'" +
            "}";
    }
}
