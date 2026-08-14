package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.kafka.common.utils.Utils;
import org.junit.jupiter.api.Test;

import us.dot.its.jpo.geojsonconverter.serialization.serializers.JsonSerializer;

public class RsuTimPartitionerTest {

    private static final String TOPIC = "topic.ProcessedTim";
    private static final int NUM_PARTITIONS = 37;

    @Test
    public void testKeysWithSameRsuUseSamePartition() {
        RsuTimKey firstKey = new RsuTimKey("192.0.2.10", "packet-a", 1);
        RsuTimKey secondKey = new RsuTimKey("192.0.2.10", "packet-b", 2);
        var partitioner = new RsuTimPartitioner<RsuTimKey, Object>();

        assertEquals(partitioner.partition(TOPIC, firstKey, null, NUM_PARTITIONS),
                partitioner.partition(TOPIC, secondKey, null, NUM_PARTITIONS));
    }

    @Test
    public void testMissingRsuFallsBackToFullKeyHash() {
        RsuTimKey key = new RsuTimKey(null, "packet-a", 1);
        var serializer = new JsonSerializer<RsuTimKey>();
        byte[] serializedKey = serializer.serialize(TOPIC, key);
        int expectedPartition = Utils.toPositive(Utils.murmur2(serializedKey)) % NUM_PARTITIONS;
        var partitioner = new RsuTimPartitioner<RsuTimKey, Object>();

        assertEquals(expectedPartition, partitioner.partition(TOPIC, key, null, NUM_PARTITIONS));
    }
}
