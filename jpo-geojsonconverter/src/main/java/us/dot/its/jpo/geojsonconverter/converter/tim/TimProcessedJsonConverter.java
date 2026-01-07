package us.dot.its.jpo.geojsonconverter.converter.tim;

import us.dot.its.jpo.geojsonconverter.partitioner.RsuTimKey;
import us.dot.its.jpo.geojsonconverter.pojos.common.DeserializedRawMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.tim.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;

/**
 * Converts ODE TIM messages to Processed TIM GeoJSON format.
 * 
 * This converter processes Traveler Information Messages (TIM) from ASN.1 format and generates GeoJSON features with
 * appropriate geometries: - Path regions become LineString or MultiLineString - Circle/closed regions become Polygon or
 * MultiPolygon - Multiple regions are combined into MultiLineString or MultiPolygon as appropriate
 * 
 * @deprecated This class has been refactored into separate classes for better maintainability. Use
 *             {@link TimTransformer} for Kafka Streams operations and {@link TimConverter} for conversion logic.
 */
@Slf4j
@Deprecated
public class TimProcessedJsonConverter
        implements Transformer<Void, DeserializedRawMessageFrame, KeyValue<RsuTimKey, ProcessedTim>> {

    private final TimTransformer timTransformer;

    public TimProcessedJsonConverter(TimConverter timConverter) {
        this.timTransformer = new TimTransformer(timConverter);
    }

    @Override
    public void init(ProcessorContext context) {
        timTransformer.init(context);
    }

    /**
     * Transform an ODE TIM POJO to Processed TIM POJO.
     *
     * @param rawKey Void type because ODE topics have no specified key
     * @param rawTim The raw POJO containing TIM data
     * @return A key-value pair: the key is an {@link RsuTimKey} containing the RSU IP address, packet ID, and message
     *         count, and the value is the ProcessedTim POJO
     */
    @Override
    public KeyValue<RsuTimKey, ProcessedTim> transform(Void rawKey, DeserializedRawMessageFrame rawTim) {
        return timTransformer.transform(rawKey, rawTim);
    }

    @Override
    public void close() {
        timTransformer.close();
    }
}
