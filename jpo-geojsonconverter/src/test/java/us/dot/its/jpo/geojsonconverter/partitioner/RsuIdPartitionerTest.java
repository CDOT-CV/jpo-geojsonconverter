package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.util.Optional;
import java.util.Set;

public class RsuIdPartitionerTest {

    
    
    @Test
    public void testPartition_RsuIdKey() {

        final String topic = "topic";

        final String ip1 = "1.1.1.1";
        final String ip2 = "2.2.2.2";

        final int intersection1 = 11111;
        final int intersection2 = 22222;
         
        var key11 = new RsuIntersectionKey(ip1, intersection1);
        var key12 = new RsuIntersectionKey(ip1, intersection2);
        var key21 = new RsuIntersectionKey(ip2, intersection1);
        var key22 = new RsuIntersectionKey(ip2, intersection2);

        final Object obj = new Object();

        // Minimize possibility of collisions
        final int numPartitions = Integer.MAX_VALUE;

        var partitioner = new RsuIdPartitioner<RsuIntersectionKey, Object>();

        Optional<Set<Integer>> partition11 = partitioner.partitions(topic, key11, obj, numPartitions);
        Optional<Set<Integer>> partition12 = partitioner.partitions(topic, key12, obj, numPartitions);
        Optional<Set<Integer>> partition21 = partitioner.partitions(topic, key21, obj, numPartitions);
        Optional<Set<Integer>> partition22 = partitioner.partitions(topic, key22, obj, numPartitions);

        String equalMsg =  "Keys with the same RSU ID should have the same partition number";
        assertEquals(equalMsg, partition11, partition12);
        assertEquals(equalMsg, partition21, partition22);

        String notEqualMsg = "Keys with different RSU IDs are unlikely to have the same partition number";
        assertNotEquals(notEqualMsg, partition11, partition21);
        assertNotEquals(notEqualMsg, partition11, partition22);
        assertNotEquals(notEqualMsg, partition12, partition21);
        assertNotEquals(notEqualMsg, partition12, partition22);
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

        var partitioner = new RsuIdPartitioner<String, Object>();

        Optional<Set<Integer>> partitionKey = partitioner.partitions(topic, key, obj, numPartitions);
        Optional<Set<Integer>> partitionSame = partitioner.partitions(topic, sameKey, obj, numPartitions);
        Optional<Set<Integer>> partitionDifferent = partitioner.partitions(topic, differentKey, obj, numPartitions);

        assertEquals("Same keys", partitionKey, partitionSame);
        assertNotEquals("Different keys", partitionKey, partitionDifferent);
    }

    
}
