package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RsuLogKeyTest {
    final static String ipAddress = "127.0.0.1";
    final static String logFileName = "bsmLogDuringEvent_commsignia.gz";
    final static String bsmId = "ABCDEFG";
    
    @Test
    public void testEquality() {        
        
        var key = new RsuLogKey();
        key.setRsuId(ipAddress);
        key.setLogId(logFileName);
        key.setBsmId(bsmId);

        var keyValue = new RsuLogKey(ipAddress, logFileName, bsmId);
        var keyRef = key;
        Object otherObject = new Object();
        var otherValue1 = new RsuLogKey(ipAddress, null, bsmId);
        var otherValue2 = new RsuLogKey("0.0.0.0", "", bsmId);
        var otherValue3 = new RsuLogKey(ipAddress, "bsmTx.gz", bsmId);

        assertTrue(key.equals(keyValue), "Value equality");
        assertFalse(key.equals(otherValue1), "Value inequality branch 1");
        assertFalse(key.equals(otherValue2), "Value inequality branch 2");
        assertFalse(key.equals(otherValue3), "Value inequality branch 3");
        assertTrue(key.equals(keyRef), "Reference equality");
        assertFalse(key.equals(otherObject), "Reference inequality");
        assertEquals(key.hashCode(), keyValue.hashCode(), "Hash code values equal");

        // Getter coverage
        assertEquals(key.getRsuId(), keyValue.getRsuId(), "getRsuId");
        assertEquals(key.getLogId(), keyValue.getLogId(), "getLogId");
        assertEquals(key.getBsmId(), keyValue.getBsmId(), "getBsmId");
    }

    @Test
    public void testToString() {
        var key = new RsuLogKey();
        key.setRsuId(ipAddress);
        key.setLogId(logFileName);
        key.setBsmId(bsmId);

        String str = key.toString();
        assertTrue(str.contains(ipAddress));
        assertTrue(str.contains(logFileName));
        assertTrue(str.contains(bsmId));
    }
}
