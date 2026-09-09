package us.dot.its.jpo.geojsonconverter.converter.tim;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimPartitioner;
import us.dot.its.jpo.geojsonconverter.pojos.common.DeserializedRawMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.common.Ieee1609Dot2MetadataExtractor;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.geojsonconverter.validator.TimJsonValidator;

/**
 * Kafka Streams Topology builder for processing TIM messages from ODE TIM JSON -> Processed TIM
 */
@Slf4j
public class TimTopology {

    public static Topology build(String timOdeJsonTopic, String timProcessedJsonTopic,
            TimJsonValidator timJsonValidator, TimConverter timConverter) {
        StreamsBuilder builder = new StreamsBuilder();

        // Stream for raw TIM messages
        // Raw topic has no key and the values are raw JSON bytes
        KStream<Void, Bytes> rawOdeTimStream =
                builder.stream(timOdeJsonTopic, Consumed.with(Serdes.Void(), Serdes.Bytes()));

        // Validate the JSON and write validation errors to the log at warn level
        // Passes the raw JSON along unchanged, even if there are validation errors.
        KStream<Void, DeserializedRawMessageFrame> validatedOdeTimStream =
                rawOdeTimStream.mapValues((Void key, Bytes value) -> {
                    DeserializedRawMessageFrame deserializedRawMessageFrame = new DeserializedRawMessageFrame();
                    try (var serde = JsonSerdes.OdeMessageFrame()) {
                        JsonValidatorResult validationResults = timJsonValidator.validate(value.get());
                        deserializedRawMessageFrame
                                .setOdeMessageFrameData(serde.deserializer().deserialize(timOdeJsonTopic, value.get()));
                        deserializedRawMessageFrame.setSignedDataMetadata(
                                Ieee1609Dot2MetadataExtractor.extractSignedDataMetadata(value.get()));
                        deserializedRawMessageFrame.setValidationResults(validationResults);
                        log.debug(validationResults.describeResults());
                    } catch (Exception e) {
                        JsonValidatorResult validatorResult = new JsonValidatorResult();

                        validatorResult.addException(e);
                        deserializedRawMessageFrame.setValidationFailure(true);
                        deserializedRawMessageFrame.setValidationResults(validatorResult);
                        deserializedRawMessageFrame.setFailedMessage(e.getMessage());

                        log.error("Error in timValidation:", e);
                    }
                    return deserializedRawMessageFrame;
                });

        // Convert ODE TIM to ProcessedTim
        KStream<RsuTimKey, ProcessedTim> processedJsonTimStream =
                validatedOdeTimStream.map(new TimTransformer(timConverter));

        // Removes null messages from being posted to output topic.
        // Helpful to remove generated messages that caused exceptions.
        processedJsonTimStream = processedJsonTimStream.filter((key, value) -> value != null);

        processedJsonTimStream.to(
                // Push the ProcessedTim to the output topic partitioned by RsuTimKey
                timProcessedJsonTopic, Produced.with(JsonSerdes.RsuTimKey(), JsonSerdes.ProcessedTim(),
                        new RsuTimPartitioner<RsuTimKey, ProcessedTim>()));

        return builder.build();
    }
}
