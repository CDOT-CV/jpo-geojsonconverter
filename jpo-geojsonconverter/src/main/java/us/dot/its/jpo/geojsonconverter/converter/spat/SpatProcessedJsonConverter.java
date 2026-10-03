package us.dot.its.jpo.geojsonconverter.converter.spat;

import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.Common.DSecond;
import us.dot.its.jpo.asn.j2735.r2024.Common.IntersectionReferenceID;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;
import us.dot.its.jpo.asn.j2735.r2024.Common.SpeedConfidence;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.*;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedIntersectionReferenceID;
import us.dot.its.jpo.geojsonconverter.pojos.common.ProcessedSpeedConfidence;
import us.dot.its.jpo.geojsonconverter.pojos.spat.*;
import us.dot.its.jpo.geojsonconverter.utils.BitstringUtils;
import us.dot.its.jpo.geojsonconverter.utils.J2735DateTimeConverter;
import us.dot.its.jpo.geojsonconverter.utils.ProcessedSchemaVersions;
import us.dot.its.jpo.geojsonconverter.validator.CTI4501Validator;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.schema.Error;

@Slf4j
public class SpatProcessedJsonConverter
        implements KeyValueMapper<Void, DeserializedRawSpat, KeyValue<RsuIntersectionKey, ProcessedSpat>> {
    private static final Logger logger = LoggerFactory.getLogger(SpatProcessedJsonConverter.class);

    /**
     * Apply the conversion from an ODE SPaT POJO to SPaT GeoJSON POJO.
     *
     * @param rawKey - Void type because ODE topics have no specified key
     * @param rawSpat - The raw POJO
     * @return A key value pair: the key an {@link RsuIntersectionKey} containing the RSU IP address and Intersection ID
     *         and the value is the GeoJSON FeatureCollection POJO
     */
    @Override
    public KeyValue<RsuIntersectionKey, ProcessedSpat> apply(Void rawKey, DeserializedRawSpat rawSpat) {
        try {
            if (!rawSpat.isValidationFailure()) {
                OdeMessageFrameData rawValue = new OdeMessageFrameData();
                rawValue.setMetadata(rawSpat.getOdeSpatMessageFrameData().getMetadata());
                OdeMessageFrameMetadata spatMetadata = rawValue.getMetadata();

                rawValue.setPayload(rawSpat.getOdeSpatMessageFrameData().getPayload());
                SPATMessageFrame spatMessageFrame = (SPATMessageFrame) rawValue.getPayload().getData();

                ProcessedSpat processedSpat =
                        createProcessedSpat(spatMessageFrame.getValue(), spatMetadata, rawSpat.getValidatorResults());

                // Set the schema version
                processedSpat.setSchemaVersion(ProcessedSchemaVersions.PROCESSED_SPAT_SCHEMA_VERSION);

                var key = new RsuIntersectionKey();
                key.setRsuId(spatMetadata.getOriginIp());
                key.setIntersectionReferenceID(spatMessageFrame.getValue().getIntersections().get(0).getId());
                return KeyValue.pair(key, processedSpat);
            } else {
                ProcessedSpat processedSpat =
                        createFailureProcessedSpat(rawSpat.getValidatorResults(), rawSpat.getFailedMessage());
                var key = new RsuIntersectionKey();
                key.setRsuId("ERROR");

                return KeyValue.pair(key, processedSpat);
            }
        } catch (Exception e) {
            String errMsg =
                    String.format("Exception converting ODE SPaT to Processed SPaT! Message: %s", e.getMessage());
            logger.error(errMsg, e);
            // KafkaStreams knows to remove null responses before allowing further steps from occurring
            var key = new RsuIntersectionKey();
            key.setRsuId("ERROR");
            return KeyValue.pair(key, null);
        }
    }


    public ProcessedSpat createProcessedSpat(SPAT spat, OdeMessageFrameMetadata metadata,
            JsonValidatorResult validationMessages) {
        IntersectionState intersectionState = spat.getIntersections().get(0);
        ProcessedSpat processedSpat = new ProcessedSpat();
        processedSpat.setOdeReceivedAt(metadata.getOdeReceivedAt()); // ISO 8601: 2022-11-11T16:36:10.529530Z
        processedSpat.setOriginIp(metadata.getOriginIp());
        processedSpat.setAsn1(metadata.getAsn1());
        processedSpat.setName(intersectionState.getName() != null ? intersectionState.getName().getValue() : null);
        processedSpat.setIntersectionReferenceID(convertIntersectionReferenceId(intersectionState.getId()));
        List<ProcessedValidationMessage> processedSpatValidationMessages =
                createValidationMessages(spat, validationMessages);
        processedSpat.setValidationMessages(processedSpatValidationMessages);
        processedSpat.setCti4501Conformant(processedSpatValidationMessages.isEmpty());
        processedSpat.setRevision(
                intersectionState.getRevision() != null ? (int) intersectionState.getRevision().getValue() : null);
        ProcessedIntersectionStatusObject processedStatus = new ProcessedIntersectionStatusObject();
        BitstringUtils.processBitstring(processedStatus, intersectionState.getStatus());
        processedSpat.setStatus(processedStatus);
        processedSpat.setEnabledLanes(convertEnabledLanes(intersectionState));
        ZonedDateTime utcTimestamp = resolveUtcTimestamp(spat, intersectionState, metadata);
        processedSpat.setUtcTimeStamp(utcTimestamp);
        processedSpat.setStates(convertMovementStates(intersectionState, utcTimestamp));
        return processedSpat;
    }

    private ProcessedIntersectionReferenceID convertIntersectionReferenceId(
            IntersectionReferenceID intersectionReferenceID) {
        ProcessedIntersectionReferenceID processedId = new ProcessedIntersectionReferenceID();
        processedId.setId(intersectionReferenceID.getId() != null
                ? (int) intersectionReferenceID.getId().getValue()
                : null);
        processedId.setRegion(intersectionReferenceID.getRegion() != null
                ? (int) intersectionReferenceID.getRegion().getValue()
                : null);
        return processedId;
    }

    private List<ProcessedValidationMessage> createValidationMessages(SPAT spat,
            JsonValidatorResult validationMessages) {
        List<ProcessedValidationMessage> processedMessages = new ArrayList<>();
        for (Exception exception : validationMessages.getExceptions()) {
            ProcessedValidationMessage message = new ProcessedValidationMessage();
            message.setMessage(exception.getMessage());
            message.setException(exception.getStackTrace().toString());
            processedMessages.add(message);
        }
        for (Error validationMessage : validationMessages.getValidationMessages()) {
            ProcessedValidationMessage message = new ProcessedValidationMessage();
            message.setMessage(validationMessage.getMessage());
            final var schemaLocation = validationMessage.getSchemaLocation();
            if (schemaLocation != null) {
                message.setSchemaPath(schemaLocation.toString());
            } else {
                log.warn("validationMessage.schemaLocation is null");
            }
            final var evaluationPath = validationMessage.getEvaluationPath();
            if (evaluationPath != null) {
                message.setJsonPath(evaluationPath.toString());
            }
            processedMessages.add(message);
        }
        processedMessages.addAll(CTI4501Validator.spatValidation(spat));
        return processedMessages;
    }

    private List<Integer> convertEnabledLanes(IntersectionState intersectionState) {
        List<Integer> enabledLanes = new ArrayList<>();
        if (intersectionState.getEnabledLanes() != null) {
            enabledLanes.addAll(
                    intersectionState.getEnabledLanes().stream().map(laneId -> (int) laneId.getValue()).toList());
        }
        return enabledLanes;
    }

    private ZonedDateTime resolveUtcTimestamp(SPAT spat, IntersectionState intersectionState,
            OdeMessageFrameMetadata metadata) {
        MinuteOfTheYear spatMoy = spat.getTimeStamp();
        MinuteOfTheYear intersectionMoy = intersectionState.getMoy();
        DSecond intersectionDSecond = intersectionState.getTimeStamp();
        ZonedDateTime odeDate = Instant.parse(metadata.getOdeReceivedAt()).atZone(ZoneId.of("UTC"));
        return intersectionMoy != null
                ? J2735DateTimeConverter.generateUTCTimestamp(intersectionMoy, intersectionDSecond, odeDate)
                : J2735DateTimeConverter.generateUTCTimestamp(spatMoy, intersectionDSecond, odeDate);
    }

    private List<ProcessedMovementState> convertMovementStates(IntersectionState intersectionState,
            ZonedDateTime utcTimestamp) {
        List<ProcessedMovementState> processedMovementStateList = new ArrayList<ProcessedMovementState>();
        for (MovementState signalGroupState : intersectionState.getStates()) {
            ProcessedMovementState processedMovementState = new ProcessedMovementState();
            processedMovementState.setMovementName(
                    signalGroupState.getMovementName() != null ? signalGroupState.getMovementName().getValue() : null);
            processedMovementState.setSignalGroup(
                    signalGroupState.getSignalGroup() != null ? (int) signalGroupState.getSignalGroup().getValue()
                            : null);

            List<ProcessedMovementEvent> processedMovementEventList = new ArrayList<ProcessedMovementEvent>();
            if (signalGroupState.getState_time_speed() != null) {
                for (MovementEvent incomingMovementEvent : signalGroupState.getState_time_speed()) {
                    processedMovementEventList.add(convertMovementEvent(incomingMovementEvent, utcTimestamp));
                }
            }
            processedMovementState.setStateTimeSpeed(processedMovementEventList);
            processedMovementStateList.add(processedMovementState);
        }
        return processedMovementStateList;
    }

    private ProcessedMovementEvent convertMovementEvent(MovementEvent movementEvent, ZonedDateTime utcTimestamp) {
        ProcessedMovementEvent processedEvent = new ProcessedMovementEvent();
        MovementPhaseState phaseState = movementEvent.getEventState();
        if (phaseState != null) {
            processedEvent.setEventState(ProcessedMovementPhaseState.fromName(phaseState.getName()));
        }
        processedEvent.setTiming(convertTimingDetails(movementEvent, utcTimestamp));
        processedEvent.setSpeeds(convertAdvisorySpeedList(movementEvent.getSpeeds()));
        return processedEvent;
    }

    private TimingChangeDetails convertTimingDetails(MovementEvent movementEvent, ZonedDateTime utcTimestamp) {
        var timing = movementEvent.getTiming();
        TimingChangeDetails timingDetails = new TimingChangeDetails();
        TimeMark startTime = timing.getStartTime();
        TimeMark minEndTime = timing.getMinEndTime();
        TimeMark maxEndTime = timing.getMaxEndTime();
        TimeMark likelyTime = timing.getLikelyTime();
        TimeMark nextTime = timing.getNextTime();
        timingDetails.setStartTime(
                J2735DateTimeConverter.generateStartUTCTimestampForTimeMark(utcTimestamp, startTime, minEndTime));
        timingDetails.setMinEndTime(
                J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, minEndTime));
        timingDetails.setMaxEndTime(
                J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, maxEndTime));
        timingDetails.setLikelyTime(
                J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, likelyTime));
        timingDetails.setNextTime(
                J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, nextTime));
        timingDetails.setConfidence(timing.getConfidence() != null ? (int) timing.getConfidence().getValue() : null);
        return timingDetails;
    }

    private ProcessedAdvisorySpeedList convertAdvisorySpeedList(AdvisorySpeedList advisorySpeedList) {
        if (advisorySpeedList == null) {
            return null;
        }
        ProcessedAdvisorySpeedList processedAdvisorySpeedList = new ProcessedAdvisorySpeedList();
        for (AdvisorySpeed advisorySpeed : advisorySpeedList) {
            processedAdvisorySpeedList.add(convertAdvisorySpeed(advisorySpeed));
        }
        return processedAdvisorySpeedList;
    }

    private ProcessedAdvisorySpeed convertAdvisorySpeed(AdvisorySpeed advisorySpeed) {
        ProcessedAdvisorySpeed processedAdvisorySpeed = new ProcessedAdvisorySpeed();
        processedAdvisorySpeed.setSpeed(
                advisorySpeed.getSpeed() != null ? (int) advisorySpeed.getSpeed().getValue() : null);
        processedAdvisorySpeed.setClass_(advisorySpeed.getClass_() != null
                ? (int) advisorySpeed.getClass_().getValue()
                : null);
        processedAdvisorySpeed.setDistance(advisorySpeed.getDistance() != null
                ? (int) advisorySpeed.getDistance().getValue()
                : null);
        AdvisorySpeedType advisorySpeedType = advisorySpeed.getType();
        if (advisorySpeedType != null) {
            processedAdvisorySpeed.setType(ProcessedAdvisorySpeedType.fromName(advisorySpeedType.getName()));
        }
        SpeedConfidence speedConfidence = advisorySpeed.getConfidence();
        if (speedConfidence != null) {
            processedAdvisorySpeed.setConfidence(ProcessedSpeedConfidence.fromName(speedConfidence.getName()));
        }
        return processedAdvisorySpeed;
    }

    public ProcessedSpat createFailureProcessedSpat(JsonValidatorResult validatorResult, String message) {
        ProcessedSpat processedSpat = new ProcessedSpat();
        ProcessedValidationMessage object = new ProcessedValidationMessage();
        List<ProcessedValidationMessage> processedSpatValidationMessages = new ArrayList<ProcessedValidationMessage>();

        ZonedDateTime utcDateTime = ZonedDateTime.now(ZoneOffset.UTC);

        object.setMessage(message);
        object.setException(ExceptionUtils.getStackTrace(validatorResult.getExceptions().get(0)));

        processedSpatValidationMessages.add(object);
        processedSpat.setValidationMessages(processedSpatValidationMessages);
        processedSpat.setUtcTimeStamp(utcDateTime);

        return processedSpat;
    }
}
