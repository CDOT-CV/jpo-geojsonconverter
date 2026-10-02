package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RsuLogKeyTest {
    static final String IP_ADDRESS = "127.0.0.1";
    static final String LOG_FILE_NAME = "bsmLogDuringEvent_commsignia.gz";
    static final String BSM_ID = "ABCDEFG";
    
    @Test
    public void testEquality() {        
        
        var key = new RsuLogKey();
        key.setRsuId(IP_ADDRESS);
        key.setLogId(LOG_FILE_NAME);
        key.setBsmId(BSM_ID);

        var keyValue = new RsuLogKey(IP_ADDRESS, LOG_FILE_NAME, BSM_ID);
        var keyRef = key;
        Object otherObject = new Object();
        var otherValue1 = new RsuLogKey(IP_ADDRESS, null, BSM_ID);
        var otherValue2 = new RsuLogKey("0.0.0.0", "", BSM_ID);
        var otherValue3 = new RsuLogKey(IP_ADDRESS, "bsmTx.gz", BSM_ID);

        assertEquals(keyValue, key, "Value equality");
        assertNotEquals(otherValue1, key, "Value inequality branch 1");
        assertNotEquals(otherValue2, key, "Value inequality branch 2");
        assertNotEquals(otherValue3, key, "Value inequality branch 3");
        assertTrue(key.equals(keyRef), "Reference equality");
        assertNotEquals(otherObject, key, "Reference inequality");
        assertEquals(key.hashCode(), keyValue.hashCode(), "Hash code values equal");

        // Getter coverage
        assertEquals(IP_ADDRESS, key.getRsuId(), "getRsuId");
        assertEquals(LOG_FILE_NAME, key.getLogId(), "getLogId");
        assertEquals(BSM_ID, key.getBsmId(), "getBsmId");
    }

    @Test
    public void testToString() {
        var key = new RsuLogKey();
        key.setRsuId(IP_ADDRESS);
        key.setLogId(LOG_FILE_NAME);
        key.setBsmId(BSM_ID);

        String str = key.toString();
        assertTrue(str.contains(IP_ADDRESS));
        assertTrue(str.contains(LOG_FILE_NAME));
        assertTrue(str.contains(BSM_ID));
    }
}
