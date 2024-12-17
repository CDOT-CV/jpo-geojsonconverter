package us.dot.its.jpo.geojsonconverter.converter.tim;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuLogKey;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuLogKeyPartitioner;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.DeserializedRawTim;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes;
import us.dot.its.jpo.geojsonconverter.validator.TimJsonValidator;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;

/**
 * Kafka Streams Topology builder for processing TIM messages from
 * ODE TIM JSON -> TIM GeoJSON
 */
public class TimTopology {

    private static final Logger logger = LoggerFactory.getLogger(TimTopology.class);

    public static Topology build(String timOdeJsonTopic, String timProcessedJsonTopic, TimJsonValidator timJsonValidator) {
        StreamsBuilder builder = new StreamsBuilder();

        // Stream for raw TIM messages
        KStream<Void, Bytes> rawOdeTimStream =
            builder.stream(
                timOdeJsonTopic,
                Consumed.with(
                    Serdes.Void(),  // Raw topic has no key
                    Serdes.Bytes()  // Raw JSON bytes
                )
            );

        // Validate the JSON and write validation errors to the log at warn level
        // Passes the raw JSON along unchanged, even if there are validation errors.
        KStream<Void, DeserializedRawTim> validatedOdeTimStream = 
            rawOdeTimStream.mapValues(
                (Void key, Bytes value) -> {
                    DeserializedRawTim deserializedRawTim = new DeserializedRawTim();
                    try {
                        JsonValidatorResult validationResults = timJsonValidator.validate(value.get());
                        deserializedRawTim.setOdeTimData(JsonSerdes.OdeTim().deserializer().deserialize(timOdeJsonTopic, value.get()));
                        deserializedRawTim.setValidatorResults(validationResults);
                        logger.debug(validationResults.describeResults());
                    } catch (Exception e) {
                        JsonValidatorResult validatorResult = new JsonValidatorResult();

                        validatorResult.addException(e);
                        deserializedRawTim.setValidationFailure(true);
                        deserializedRawTim.setValidatorResults(validatorResult);
                        deserializedRawTim.setFailedMessage(e.getMessage());

                        logger.error("Error in timValidation:", e);
                    }
                    return deserializedRawTim;
                }
            );

        // Convert ODE TIM to GeoJSON
        KStream<RsuLogKey, ProcessedTim<Point>> processedJsonTimStream =
            validatedOdeTimStream.transform(
                () -> new TimProcessedJsonConverter()
            );

            processedJsonTimStream.to(
            // Push the joined GeoJSON stream back out to the TIM GeoJSON topic 
            timProcessedJsonTopic, 
            Produced.with(
                JsonSerdes.RsuLogKey(),
                JsonSerdes.ProcessedTim(),
                new RsuLogKeyPartitioner<RsuLogKey, ProcessedTim<Point>>())
        );
        
        return builder.build();
    }
}
