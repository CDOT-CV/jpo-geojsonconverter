package us.dot.its.jpo.geojsonconverter.converter.tim;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.streams.kstream.KStream;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimPartitioner;
import us.dot.its.jpo.geojsonconverter.pojos.common.DeserializedRawMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.geojsonconverter.validator.TimJsonValidator;

/**
 * Kafka Streams Topology builder for processing TIM messages from ODE TIM JSON -> TIM GeoJSON
 */
public class TimTopology {

    private static final Logger logger = LoggerFactory.getLogger(TimTopology.class);

    public static Topology build(String timOdeJsonTopic, String timProcessedJsonTopic,
            TimJsonValidator timJsonValidator) {
        StreamsBuilder builder = new StreamsBuilder();

        // Stream for raw TIM messages
        // Raw topic has no key and the values are raw JSON bytes
        KStream<Void, Bytes> rawOdeTimStream =
                builder.stream(timOdeJsonTopic, Consumed.with(Serdes.Void(), Serdes.Bytes()));

        // Validate the JSON and write validation errors to the log at warn level
        // Passes the raw JSON along unchanged, even if there are validation errors.
        KStream<Void, DeserializedRawMessageFrame> validatedOdeTimStream =
                rawOdeTimStream.mapValues((Void key, Bytes value) -> {
                    DeserializedRawMessageFrame DeserializedRawMessageFrame = new DeserializedRawMessageFrame();
                    try {
                        JsonValidatorResult validationResults = timJsonValidator.validate(value.get());
                        DeserializedRawMessageFrame.setOdeMessageFrameData(
                                JsonSerdes.OdeMessageFrame().deserializer().deserialize(timOdeJsonTopic, value.get()));

                        DeserializedRawMessageFrame.setValidationResults(validationResults);
                        logger.debug(validationResults.describeResults());
                    } catch (Exception e) {
                        JsonValidatorResult validatorResult = new JsonValidatorResult();

                        validatorResult.addException(e);
                        DeserializedRawMessageFrame.setValidationFailure(true);
                        DeserializedRawMessageFrame.setValidationResults(validatorResult);
                        DeserializedRawMessageFrame.setFailedMessage(e.getMessage());

                        logger.error("Error in timValidation:", e);
                    }
                    return DeserializedRawMessageFrame;
                });

        // Convert ODE TIM to ProcessedTim which is not GeoJSON
        KStream<RsuTimKey, ProcessedTim> processedJsonTimStream = validatedOdeTimStream.transform(() -> {
            TimGeometryConverter geometryProcessor = new TimGeometryConverter();
            TimConverter timConverter = new TimConverter(geometryProcessor);
            return new TimTransformer(timConverter);
        });

        processedJsonTimStream.to(
                // Push the ProcessedTim to the output topic partitioned by RsuTimKey
                timProcessedJsonTopic, Produced.with(JsonSerdes.RsuTimKey(), JsonSerdes.ProcessedTim(),
                        new RsuTimPartitioner<RsuTimKey, ProcessedTim>()));

        return builder.build();
    }
}
