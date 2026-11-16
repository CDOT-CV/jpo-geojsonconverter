package us.dot.its.jpo.geojsonconverter.converter.tim;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.pojos.common.DeserializedRawMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.tim.*;
import us.dot.its.jpo.geojsonconverter.utils.ProcessedSchemaVersions;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;

/**
 * {@link KeyValueMapper} for converting ODE TIM messages to Processed TIM format.
 *
 * <p>
 * This mapper handles the Kafka Streams specific operations and delegates the actual conversion logic to the
 * {@link TimConverter} class.
 */
@Slf4j
public class TimTransformer
        implements KeyValueMapper<Void, DeserializedRawMessageFrame, KeyValue<RsuTimKey, ProcessedTim>> {

    private static final String ERROR_RSU_ID = "ERROR";

    private final TimConverter timConverter;

    public TimTransformer(TimConverter timConverter) {
        this.timConverter = timConverter;
    }

    /**
     * Apply the conversion from an ODE TIM POJO to Processed TIM POJO.
     *
     * @param rawKey Void type because ODE topics have no specified key
     * @param rawTim The raw POJO containing TIM data
     * @return A key-value pair: the key is an {@link RsuTimKey} containing the RSU IP address, packet ID, and message
     *         count, and the value is the ProcessedTim POJO
     */
    @Override
    public KeyValue<RsuTimKey, ProcessedTim> apply(Void rawKey, DeserializedRawMessageFrame rawTim) {
        try {
            if (!rawTim.isValidationFailure()) {
                return processValidTim(rawTim);
            } else {
                return processInvalidTim(rawTim);
            }
        } catch (Exception e) {
            log.error("Exception converting ODE TIM to Processed TIM: {}", e.getMessage(), e);
            return createErrorKeyValuePair();
        }
    }

    /**
     * Process a valid TIM message.
     *
     * @param rawTim The valid TIM data
     * @return Key-value pair with processed TIM
     */
    private KeyValue<RsuTimKey, ProcessedTim> processValidTim(DeserializedRawMessageFrame rawTim) {
        OdeMessageFrameData rawValue = rawTim.getOdeMessageFrameData();
        OdeMessageFrameMetadata timMetadata = rawValue.getMetadata();

        TravelerInformationMessageFrame travelerInfoMessageFrame =
                (TravelerInformationMessageFrame) rawValue.getPayload().getData();

        ProcessedTim processedTim = timConverter.createProcessedTim(travelerInfoMessageFrame.getValue(), timMetadata);
        processedTim.setSchemaVersion(ProcessedSchemaVersions.PROCESSED_TIM_SCHEMA_VERSION);

        // Apply validation results
        timConverter.jsonValidation(processedTim, rawTim.getValidationResults());

        // Create key with TIM-specific data
        TravelerInformation travelerInfo = travelerInfoMessageFrame.getValue();
        String packetId = null;
        Integer msgCnt = null;

        if (travelerInfo.getPacketID() != null) {
            packetId = travelerInfo.getPacketID().getValue();
        }
        if (travelerInfo.getMsgCnt() != null) {
            msgCnt = (int) travelerInfo.getMsgCnt().getValue();
        }

        RsuTimKey key = createRsuTimKey(timMetadata.getOriginIp(), packetId, msgCnt);
        return KeyValue.pair(key, processedTim);
    }

    /**
     * Process an invalid TIM message.
     *
     * @param rawTim The invalid TIM data
     * @return Key-value pair with failure information
     */
    private KeyValue<RsuTimKey, ProcessedTim> processInvalidTim(DeserializedRawMessageFrame rawTim) {
        ProcessedTim processedTim = timConverter.createFailureProcessedTim(rawTim.getFailedMessage());

        // Apply validation results
        timConverter.jsonValidation(processedTim, rawTim.getValidationResults());

        RsuTimKey key = createRsuTimKey(ERROR_RSU_ID);
        return KeyValue.pair(key, processedTim);
    }

    /**
     * Create an error key-value pair for exception handling.
     *
     * @return Key-value pair with error key and null value
     */
    private KeyValue<RsuTimKey, ProcessedTim> createErrorKeyValuePair() {
        RsuTimKey key = createRsuTimKey(ERROR_RSU_ID);
        return KeyValue.pair(key, null);
    }

    /**
     * Create an RSU TIM key.
     *
     * @param rsuId The RSU ID
     * @return Configured RSU TIM key
     */
    private RsuTimKey createRsuTimKey(String rsuId) {
        return new RsuTimKey(rsuId);
    }

    /**
     * Create an RSU TIM key with TIM-specific data.
     *
     * @param rsuId The RSU ID
     * @param packetId The packet ID
     * @param msgCnt The message count
     * @return Configured RSU TIM key
     */
    private RsuTimKey createRsuTimKey(String rsuId, String packetId, Integer msgCnt) {
        return new RsuTimKey(rsuId, packetId, msgCnt);
    }
}
