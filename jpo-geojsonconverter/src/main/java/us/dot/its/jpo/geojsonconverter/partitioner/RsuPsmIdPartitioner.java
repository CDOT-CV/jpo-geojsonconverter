package us.dot.its.jpo.geojsonconverter.partitioner;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.processor.StreamPartitioner;

import us.dot.its.jpo.geojsonconverter.serialization.serializers.JsonSerializer;

import java.util.Optional;
import java.util.Set;

public class RsuPsmIdPartitioner<K, V> implements StreamPartitioner<K, V> {
    @Override
    public Optional<Set<Integer>> partitions(String topic, K key, V value, int numPartitions) {
        byte[] partitionBytes;

        if (key instanceof RsuPsmIdKey) {
            // If the key is an object with an RSU ID, partition on it
            var rsuTypeIdKey = (RsuPsmIdKey) key;
            if (rsuTypeIdKey.getRsuId() != null && !rsuTypeIdKey.getRsuId().isEmpty())
                partitionBytes = serializeString(topic, rsuTypeIdKey.getRsuId());
            else if (rsuTypeIdKey.getPsmId() != null && !rsuTypeIdKey.getPsmId().isEmpty())
                partitionBytes = serializeString(topic, rsuTypeIdKey.getPsmId());
            else
                partitionBytes = serializeObj(topic, key);
        } else {
            // If the key does not have an RSU ID, partition on the hashed key as usual.
            partitionBytes = serializeObj(topic, key);
        }

        return Optional.of(Set.of(Utils.toPositive(Utils.murmur2(partitionBytes)) % numPartitions));
    }

    private byte[] serializeString(String topic, String str) {
        byte[] partitionBytes;
        try (var serializer = Serdes.String().serializer()) {
            partitionBytes = serializer.serialize(topic, str);
        }
        return partitionBytes;
    }

    private byte[] serializeObj(String topic, K obj) {
        byte[] partitionBytes;
        try (var serializer = new JsonSerializer<K>()) {
            partitionBytes = serializer.serialize(topic, obj);
        }
        return partitionBytes;
    }
}
