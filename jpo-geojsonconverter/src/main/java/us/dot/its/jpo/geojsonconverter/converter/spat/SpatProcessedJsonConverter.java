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
        // Create an IntersectionState from the SPAT for easier readability
        IntersectionState intersectionState = spat.getIntersections().get(0);

        // Build the ProcessedSpat object representing the intersection state
        ProcessedSpat processedSpat = new ProcessedSpat();
        processedSpat.setOdeReceivedAt(metadata.getOdeReceivedAt()); // ISO 8601: 2022-11-11T16:36:10.529530Z
        processedSpat.setOriginIp(metadata.getOriginIp());
        processedSpat.setAsn1(metadata.getAsn1());
        processedSpat.setName(intersectionState.getName() != null ? intersectionState.getName().getValue() : null);
        IntersectionReferenceID intersectionReferenceID = intersectionState.getId();
        ProcessedIntersectionReferenceID processedIntersectionReferenceID = new ProcessedIntersectionReferenceID();
        processedIntersectionReferenceID.setId(
                intersectionReferenceID.getId() != null ? (int) intersectionReferenceID.getId().getValue() : null);
        processedIntersectionReferenceID.setRegion(
                intersectionReferenceID.getRegion() != null ? (int) intersectionReferenceID.getRegion().getValue()
                        : null);
        processedSpat.setIntersectionReferenceID(processedIntersectionReferenceID);

        // Handle validation messages for the J2735 and CTI-4501 SPaT conformance validation
        List<ProcessedValidationMessage> processedSpatValidationMessages = new ArrayList<ProcessedValidationMessage>();
        for (Exception exception : validationMessages.getExceptions()) {
            ProcessedValidationMessage object = new ProcessedValidationMessage();
            object.setMessage(exception.getMessage());
            object.setException(exception.getStackTrace().toString());
            processedSpatValidationMessages.add(object);
        }
        for (Error vm : validationMessages.getValidationMessages()) {
            ProcessedValidationMessage object = new ProcessedValidationMessage();
            object.setMessage(vm.getMessage());
            final var schemaLocation = vm.getSchemaLocation();
            if (schemaLocation != null) {
                object.setSchemaPath(schemaLocation.toString());
            } else {
                log.warn("validationMessage.schemaLocation is null");
            }
            final var evaluationPath = vm.getEvaluationPath();
            if (evaluationPath != null) {
                object.setJsonPath(vm.getEvaluationPath().toString());
            }

            processedSpatValidationMessages.add(object);
        }
        processedSpatValidationMessages.addAll(CTI4501Validator.spatValidation(spat));
        processedSpat.setValidationMessages(processedSpatValidationMessages);
        processedSpat.setCti4501Conformant(processedSpat.getValidationMessages().size() == 0);

        processedSpat.setRevision(
                intersectionState.getRevision() != null ? (int) intersectionState.getRevision().getValue() : null);
        ProcessedIntersectionStatusObject processedStatus = new ProcessedIntersectionStatusObject();
        BitstringUtils.processBitstring(processedStatus, intersectionState.getStatus());
        processedSpat.setStatus(processedStatus);
        List<Integer> enabledLanes = new ArrayList<>();
        if (intersectionState.getEnabledLanes() != null) {
            enabledLanes.addAll(
                    intersectionState.getEnabledLanes().stream().map(laneId -> (int) laneId.getValue()).toList());
        }
        processedSpat.setEnabledLanes(enabledLanes);

        // Retrieve all relevant timestamp-based fields to calculate the UTC timestamp
        MinuteOfTheYear spatMoy = spat.getTimeStamp() != null ? spat.getTimeStamp() : null;
        MinuteOfTheYear intersectionMoy = intersectionState.getMoy() != null ? intersectionState.getMoy() : null;
        DSecond intersectionDSecond =
                intersectionState.getTimeStamp() != null ? intersectionState.getTimeStamp() : null;

        String odeTimestamp = metadata.getOdeReceivedAt();
        ZonedDateTime odeDate = Instant.parse(odeTimestamp).atZone(ZoneId.of("UTC"));
        // Generate the UTC timestamp based on the moy that isn't null and let the function handle the rest
        // If both are null or intersectionDSecond is null, the function will still use the ODE received
        // timestamp to
        // fill in the blanks
        ZonedDateTime utcTimestamp = intersectionMoy != null
                ? J2735DateTimeConverter.generateUTCTimestamp(intersectionMoy, intersectionDSecond, odeDate)
                : J2735DateTimeConverter.generateUTCTimestamp(spatMoy, intersectionDSecond, odeDate);
        processedSpat.setUtcTimeStamp(utcTimestamp);

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
                    ProcessedMovementEvent processedMovementEvent = new ProcessedMovementEvent();
                    MovementPhaseState phaseState = incomingMovementEvent.getEventState();
                    if (phaseState != null) {
                        ProcessedMovementPhaseState processedMovementPhaseState =
                                ProcessedMovementPhaseState.fromName(phaseState.getName());
                        processedMovementEvent.setEventState(processedMovementPhaseState);
                    }


                    // Calculate the UTC timestamps for the movement event states and account for
                    // null values
                    TimingChangeDetails spatTimingDetails = new TimingChangeDetails();
                    TimeMark startTime = incomingMovementEvent.getTiming().getStartTime();
                    TimeMark minEndTime = incomingMovementEvent.getTiming().getMinEndTime();
                    TimeMark maxEndTime = incomingMovementEvent.getTiming().getMaxEndTime();
                    TimeMark likelyTime = incomingMovementEvent.getTiming().getLikelyTime();
                    TimeMark nextTime = incomingMovementEvent.getTiming().getNextTime();
                    spatTimingDetails.setStartTime(
                            J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, startTime));
                    spatTimingDetails.setMinEndTime(
                            J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, minEndTime));
                    spatTimingDetails.setMaxEndTime(
                            J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, maxEndTime));
                    spatTimingDetails.setLikelyTime(
                            J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, likelyTime));
                    spatTimingDetails.setNextTime(
                            J2735DateTimeConverter.generateOffsetUTCTimestampForTimeMark(utcTimestamp, nextTime));
                    spatTimingDetails.setConfidence(incomingMovementEvent.getTiming().getConfidence() != null
                            ? (int) incomingMovementEvent.getTiming().getConfidence().getValue()
                            : null);
                    processedMovementEvent.setTiming(spatTimingDetails);

                    // Set the speeds if they exist, otherwise set to null
                    processedMovementEvent.setSpeeds(convertAdvisorySpeedList(incomingMovementEvent.getSpeeds()));

                    processedMovementEventList.add(processedMovementEvent);
                }
            }
            processedMovementState.setStateTimeSpeed(processedMovementEventList);
            processedMovementStateList.add(processedMovementState);
        }

        processedSpat.setStates(processedMovementStateList);
        return processedSpat;
    }

    private ProcessedAdvisorySpeedList convertAdvisorySpeedList(AdvisorySpeedList advisorySpeedList) {
        if (advisorySpeedList != null) {
            ProcessedAdvisorySpeedList processedAdvisorySpeedList = new ProcessedAdvisorySpeedList();
            for (AdvisorySpeed advisorySpeed : advisorySpeedList) {
                ProcessedAdvisorySpeed processedAdvisorySpeed = new ProcessedAdvisorySpeed();

                Integer speed = advisorySpeed.getSpeed() != null ? (int) advisorySpeed.getSpeed().getValue() : null;
                processedAdvisorySpeed.setSpeed(speed);

                Integer class_ = advisorySpeed.getClass_() != null ? (int) advisorySpeed.getClass_().getValue() : null;
                processedAdvisorySpeed.setClass_(class_);

                Integer distance =
                        advisorySpeed.getDistance() != null ? (int) advisorySpeed.getDistance().getValue() : null;
                processedAdvisorySpeed.setDistance(distance);

                AdvisorySpeedType advisorySpeedType = advisorySpeed.getType();
                if (advisorySpeedType != null) {
                    processedAdvisorySpeed.setType(ProcessedAdvisorySpeedType.fromName(advisorySpeedType.getName()));
                }

                SpeedConfidence speedConfidence = advisorySpeed.getConfidence();
                if (speedConfidence != null) {
                    processedAdvisorySpeed.setConfidence(ProcessedSpeedConfidence.fromName(speedConfidence.getName()));
                }

                processedAdvisorySpeedList.add(processedAdvisorySpeed);
            }
            return processedAdvisorySpeedList;
        }
        return null;
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
