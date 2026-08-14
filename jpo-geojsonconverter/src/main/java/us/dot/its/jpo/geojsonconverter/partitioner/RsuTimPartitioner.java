package us.dot.its.jpo.geojsonconverter.partitioner;

import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.processor.StreamPartitioner;
import us.dot.its.jpo.geojsonconverter.serialization.serializers.JsonSerializer;

import java.nio.charset.StandardCharsets;

public class RsuTimPartitioner<K, V> implements StreamPartitioner<K, V> {

    private static final JsonSerializer<Object> JSON_SERIALIZER = new JsonSerializer<>();

    @Override
    public Integer partition(String topic, K key, V value, int numPartitions) {
        byte[] partitionBytes = null;

        if (key instanceof RsuTimKey rsuTimKey) {
            if (rsuTimKey.getRsuId() != null && !rsuTimKey.getRsuId().isEmpty()) {
                partitionBytes = rsuTimKey.getRsuId().getBytes(StandardCharsets.UTF_8);
            }
        }

        // Packet IDs are unique per TIM and therefore do not provide useful grouping. When RSU identity is absent,
        // hash the complete key to retain Kafka's normal deterministic distribution behavior.
        if (partitionBytes == null) {
            partitionBytes = JSON_SERIALIZER.serialize(topic, key);
        }

        return Utils.toPositive(Utils.murmur2(partitionBytes)) % numPartitions;
    }
}
