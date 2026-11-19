package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RsuTimKeyTest {

    @Test
    void constructorsAndAccessorsSupportAllKeyShapes() {
        RsuTimKey empty = new RsuTimKey();
        assertNull(empty.getRsuId());
        assertNull(empty.getPacketId());
        assertNull(empty.getMsgCnt());

        empty.setRsuId("rsu-a");
        empty.setPacketId("packet-a");
        empty.setMsgCnt(7);
        assertEquals(new RsuTimKey("rsu-a", "packet-a", 7), empty);

        RsuTimKey full = new RsuTimKey("rsu-a", "packet-a", 7);
        RsuTimKey packetOnly = new RsuTimKey("rsu-a", "packet-a");
        RsuTimKey rsuOnly = new RsuTimKey("rsu-a");
        assertEquals(7, full.getMsgCnt());
        assertNull(packetOnly.getMsgCnt());
        assertNull(rsuOnly.getPacketId());
        assertNull(rsuOnly.getMsgCnt());
        assertTrue(full.toString().contains("rsu-a"));
        assertEquals(full, new RsuTimKey("rsu-a", "packet-a", 7));
        assertEquals(full.hashCode(), new RsuTimKey("rsu-a", "packet-a", 7).hashCode());
        assertNotEquals(full, packetOnly);
        assertNotEquals(full, rsuOnly);
        assertNotEquals(full, new RsuTimKey("rsu-b", "packet-a", 7));
        assertNotEquals(full, new RsuTimKey("rsu-a", "packet-b", 7));
        assertNotEquals(full, new RsuTimKey("rsu-a", "packet-a", 8));
        assertNotEquals(null, full);
        assertNotEquals(full, new Object());

        RsuTimKey nullValues = new RsuTimKey(null, null, null);
        assertEquals(nullValues, new RsuTimKey());
        assertTrue(nullValues.toString().contains("null"));
    }
}
