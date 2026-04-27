package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

public class RsuLogKeyPartitionerTest {
    @Test
    public void testPartition_RsuLogKey() {

        // Test that the custom partition function still behaves normally if the key is a RsuLogKey
        
        final String topic = "topic";
        RsuLogKey key = new RsuLogKey("127.0.0.1", null, "ABCDEFG");
        RsuLogKey sameKey = new RsuLogKey("127.0.0.1", null, "ABCDEFG");
        RsuLogKey differentKey = new RsuLogKey(null, "bsmTx.gz", "GFEDCBA");
        final Object obj = new Object();
        final int numPartitions = Integer.MAX_VALUE;

        var partitioner = new RsuLogKeyPartitioner<RsuLogKey, Object>();

        Optional<Set<Integer>> partitionKey = partitioner.partitions(topic, key, obj, numPartitions);
        Optional<Set<Integer>> partitionSame = partitioner.partitions(topic, sameKey, obj, numPartitions);
        Optional<Set<Integer>> partitionDifferent = partitioner.partitions(topic, differentKey, obj, numPartitions);

        assertEquals("Same keys", partitionKey, partitionSame);
        assertNotEquals("Different keys", partitionKey, partitionDifferent);
    }

    @Test
    public void testPartition_String() {

        // Test that the custom partition function still behaves normally if the key is a string

        final String topic = "topic";
        final String key = "AAA";
        final String sameKey = "AAA";
        final String differentKey = "BBB";
        final Object obj = new Object();
        final int numPartitions = Integer.MAX_VALUE;

        var partitioner = new RsuLogKeyPartitioner<String, Object>();

        Optional<Set<Integer>> partitionKey = partitioner.partitions(topic, key, obj, numPartitions);
        Optional<Set<Integer>> partitionSame = partitioner.partitions(topic, sameKey, obj, numPartitions);
        Optional<Set<Integer>> partitionDifferent = partitioner.partitions(topic, differentKey, obj, numPartitions);

        assertEquals("Same keys", partitionKey, partitionSame);
        assertNotEquals("Different keys", partitionKey, partitionDifferent);
    }
}
