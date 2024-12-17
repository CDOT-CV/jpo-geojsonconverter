package us.dot.its.jpo.geojsonconverter.converter.tim;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.schema.ValidationMessage;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuLogKey;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;

import us.dot.its.jpo.ode.model.OdeTimData;
import us.dot.its.jpo.ode.model.OdeTimMetadata;
import us.dot.its.jpo.ode.model.OdeTimPayload;

public class TimProcessedJsonConverter implements Transformer<Void, DeserializedRawTim, KeyValue<RsuLogKey, ProcessedTim<Point>>> {
    private static final Logger logger = LoggerFactory.getLogger(TimProcessedJsonConverter.class);

    @Override
    public void init(ProcessorContext arg0) { }

    /**
     * Transform an ODE TIM POJO to Processed TIM POJO.
     * 
     * @param rawKey - Void type because ODE topics have no specified key
     * @param rawTim - The raw POJO
     * @return A key value pair: the key a RsuLogKey containing the RSU IP address or the TIM log file name
     */
    @Override
    public KeyValue<RsuLogKey, ProcessedTim<Point>> transform(Void rawKey, DeserializedRawTim rawTim) {
        try {
            if (!rawTim.getValidationFailure()) {
                OdeTimData rawValue = new OdeTimData();
                rawValue.setMetadata(rawTim.getOdeTimData().getMetadata());
                OdeTimMetadata timMetadata = (OdeTimMetadata)rawValue.getMetadata();

                rawValue.setPayload(rawTim.getOdeTimData().getPayload());
                OdeTimPayload timPayload = (OdeTimPayload)rawValue.getPayload();

                ProcessedTim<Point> processedTim = createProcessedTim(timMetadata, timPayload, rawTim.getValidatorResults());

                // Set the schema version
                processedTim.setSchemaVersion(1);
                RsuLogKey key = new RsuLogKey();
                key.setRsuId(processedTim.getOriginIp());
                key.setLogId(processedTim.getLogName());
                key.setTimId(timPayload.getTim().getCoreData().getId());

                return KeyValue.pair(key, processedTim);
            } else {
                ProcessedTim<Point> processedTim = createFailureProcessedTim(rawTim.getValidatorResults(), rawTim.getFailedMessage());
                RsuLogKey key = new RsuLogKey();
                key.setTimId("ERROR");
                return KeyValue.pair(key, processedTim);
            }
        } catch (Exception e) {
            String errMsg = String.format("Exception converting ODE TIM to Processed TIM! Message: %s", e.getMessage());
            logger.error(errMsg, e);
            // KafkaStreams knows to remove null responses before allowing further steps from occurring
            RsuLogKey key = new RsuLogKey();
            key.setTimId("ERROR");
            return KeyValue.pair(key, null);
        }
    }

    @Override
    public void close() {
        // Nothing to do here
    }

    @SuppressWarnings("unchecked")
    public ProcessedTim<Point> createProcessedTim(OdeTimMetadata metadata, OdeTimPayload payload, JsonValidatorResult validationMessages) {
        List<TimFeature<Point>> timFeatures = new ArrayList<>();
        timFeatures.add(createTimFeature(payload));

        ProcessedTim<Point> processedTim = new ProcessedTim<Point>(timFeatures.toArray(new TimFeature[0]));
        processedTim.setOdeReceivedAt(metadata.getOdeReceivedAt()); // ISO 8601: 2022-11-11T16:36:10.529530Z

        if (metadata.getOriginIp() != null && !metadata.getOriginIp().isEmpty())
            processedTim.setOriginIp(metadata.getOriginIp());
        if (metadata.getLogFileName() != null && !metadata.getLogFileName().isEmpty())
            processedTim.setLogName(metadata.getLogFileName());

        List<ProcessedValidationMessage> processedTimValidationMessages = new ArrayList<ProcessedValidationMessage>();
        for (Exception exception : validationMessages.getExceptions()) {
            ProcessedValidationMessage object = new ProcessedValidationMessage();
            object.setMessage(exception.getMessage());
            object.setException(exception.getStackTrace().toString());
            processedTimValidationMessages.add(object);
        }
        for (ValidationMessage vm : validationMessages.getValidationMessages()){
            ProcessedValidationMessage object = new ProcessedValidationMessage();
            object.setMessage(vm.getMessage());
            object.setSchemaPath(vm.getSchemaPath());
            object.setJsonPath(vm.getPath());

            processedTimValidationMessages.add(object);
        }

        ZonedDateTime odeDate = Instant.parse(metadata.getOdeReceivedAt()).atZone(ZoneId.of("UTC"));

        processedTim.setValidationMessages(processedTimValidationMessages);
        processedTim.setTimeStamp(generateOffsetUTCTimestamp(odeDate, payload.getTim().getCoreData().getSecMark()));

        return processedTim;
    }

    public ProcessedTim<Point> createFailureProcessedTim(JsonValidatorResult validatorResult, String message) {
        ProcessedTim<Point> processedTim = new ProcessedTim<Point>(null);
        ProcessedValidationMessage object = new ProcessedValidationMessage();
        List<ProcessedValidationMessage> processedTimValidationMessages = new ArrayList<ProcessedValidationMessage>();

        ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);
        
        object.setMessage(message);
        object.setException(ExceptionUtils.getStackTrace(validatorResult.getExceptions().get(0)));

        processedTimValidationMessages.add(object);
        processedTim.setValidationMessages(processedTimValidationMessages);
        processedTim.setTimeStamp(utcDateTime);

        return processedTim;
    }

    public TimFeature<Point> createTimFeature(OdeTimPayload payload) {
        J2735TimCoreData coreData = payload.getTim().getCoreData();

        // Create the Geometry Point
        double timLong = coreData.getPosition().getLongitude().doubleValue();
        double timLat = coreData.getPosition().getLatitude().doubleValue();
        Point timPoint = new Point(timLong, timLat);

        // Create the TIM Properties
        TimProperties timProps = new TimProperties();
        timProps.setAccelSet(coreData.getAccelSet());
        timProps.setAccuracy(coreData.getAccuracy());
        timProps.setAngle(coreData.getAngle());
        timProps.setBrakes(coreData.getBrakes());
        timProps.setHeading(coreData.getHeading());
        timProps.setId(coreData.getId());
        timProps.setMsgCnt(coreData.getMsgCnt());
        timProps.setSecMark(coreData.getSecMark());
        timProps.setSize(coreData.getSize());
        timProps.setTransmission(coreData.getTransmission());

        return new TimFeature<Point>(null, timPoint, timProps);
    }

    public ZonedDateTime generateOffsetUTCTimestamp(ZonedDateTime odeReceivedAt, Integer secMark){
        try {
            if (secMark != null){
                int millis = (int)(secMark % 1000);
                int seconds = (int)(secMark / 1000);
                ZonedDateTime date = odeReceivedAt;
                if(secMark == 65535){

                    // Return UTC time zero if the Zoned Date time is marked as unknown, UTC time zero chosen so that a null value can represent an empty field in the TIM. But 65535, can represent an intentionally unidentified field.
                    return ZonedDateTime.ofInstant(Instant.ofEpochMilli(0), ZoneId.of("UTC"));
                    
                }else{
                    // If we are within 10 seconds of the next minute, and the timeMark is a large number, it probably means that the time rolled over before reception.
                    // In this case, subtract a minute from the odeReceivedAt so that the true time represents the minute in the past. 
                    if(odeReceivedAt.getSecond() < 10 && secMark > 50000){
                        date = date.minusMinutes(1);
                    }

                    date = date.withSecond(seconds);
                    date = date.withNano(0);
                    date = date.plus(millis, ChronoUnit.MILLIS);
                    return date;
                }

                
            } else {
                return null;
            }
        } catch (Exception e) {
            String errMsg = String.format("Failed to generateOffsetUTCTimestamp - TIMProcessedJsonConverter. Message: %s", e.getMessage());
            logger.error(errMsg, e);
            return null;
        }               
    }
}
