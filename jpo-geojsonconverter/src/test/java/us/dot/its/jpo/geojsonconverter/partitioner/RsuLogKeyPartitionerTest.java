package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

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

        int partitionKey = partitioner.partition(topic, key, obj, numPartitions);
        int partitionSame = partitioner.partition(topic, sameKey, obj, numPartitions);
        int partitionDifferent = partitioner.partition(topic, differentKey, obj, numPartitions);

        assertEquals(partitionKey, partitionSame, "Same keys");
        assertNotEquals(partitionKey, partitionDifferent, "Different keys");
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

        int partitionKey = partitioner.partition(topic, key, obj, numPartitions);
        int partitionSame = partitioner.partition(topic, sameKey, obj, numPartitions);
        int partitionDifferent = partitioner.partition(topic, differentKey, obj, numPartitions);

        assertEquals(partitionKey, partitionSame, "Same keys");
        assertNotEquals(partitionKey, partitionDifferent, "Different keys");
    }
}
