package us.dot.its.jpo.geojsonconverter.partitioner;

import org.apache.kafka.common.utils.Utils;
import org.apache.kafka.streams.processor.StreamPartitioner;
import us.dot.its.jpo.geojsonconverter.serialization.serializers.JsonSerializer;

import java.util.Optional;
import java.util.Set;

/**
 * Partitioner that partitions based only on Intersection ID for objects that implement the {@link IntersectionKey}
 * class.
 *
 * <p>
 * Partitioning ignores the region, so that items with or without a region defined will be on the same partition.
 * </p>
 *
 * <p>
 * Objects that don't implement the {@link IntersectionKey} interface are partitioned on the hash of the entire
 * serialized key, the same as the kafka default behavior.
 * </p>
 */
public class IntersectionIdPartitioner<K, V> implements StreamPartitioner<K, V> {

    @Override
    public Optional<Set<Integer>> partitions(String topic, K key, V value, int numPartitions) {


        if (key instanceof IntersectionKey) {
            var intersectionKey = (IntersectionKey) key;
            int intersectionId = intersectionKey.getIntersectionId();
            int intKey = intersectionId % numPartitions;

            // Kafka Partitioning will fail if the Key is negative. This may happen if the intersectionId is -1 which is
            // used if the intersection is unknown.
            if (intKey >= 0) {
                return Optional.of(Set.of(intKey));
            }


        }

        byte[] partitionBytes;
        try (var serializer = new JsonSerializer<K>()) {
            partitionBytes = serializer.serialize(topic, key);
        }
        return Optional.of(Set.of(Utils.toPositive(Utils.murmur2(partitionBytes)) % numPartitions));
    }

}
