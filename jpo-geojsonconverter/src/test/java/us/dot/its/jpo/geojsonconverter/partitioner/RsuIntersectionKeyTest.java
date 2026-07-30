package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.IntersectionID;
import us.dot.its.jpo.asn.j2735.r2024.Common.IntersectionReferenceID;
import us.dot.its.jpo.asn.j2735.r2024.Common.RoadRegulatorID;


public class RsuIntersectionKeyTest {

    final static String ipAddress = "127.0.0.1";
    final static int intersectionId = 10001;
    final static int region = 10;

    @Test
    public void testEquality() {

        var key = new RsuIntersectionKey();
        key.setRsuId(ipAddress);
        var intersectionRegion = new IntersectionReferenceID();
        intersectionRegion.setId(new IntersectionID(intersectionId));
        intersectionRegion.setRegion(new RoadRegulatorID(region));
        key.setIntersectionReferenceID(intersectionRegion);

        var keyValue = new RsuIntersectionKey(ipAddress, intersectionId, region);
        var keyRef = key;
        Object otherObject = new Object();
        var otherValue1 = new RsuIntersectionKey(ipAddress, 99);
        var otherValue2 = new RsuIntersectionKey("0.0.0.0", 99);
        var otherValue3 = new RsuIntersectionKey(ipAddress, intersectionId, 99);

        assertTrue(key.equals(keyValue), "Value equality");
        assertFalse(key.equals(otherValue1), "Value inequality branch 1");
        assertFalse(key.equals(otherValue2), "Value inequality branch 2");
        assertFalse(key.equals(otherValue3), "Value inequality branch 3");
        assertTrue(key.equals(keyRef), "Reference equality");
        assertFalse(key.equals(otherObject), "Reference inequality");
        assertEquals(key.hashCode(), keyValue.hashCode(), "Hash code values equal");

        // Getter coverage
        assertEquals(key.getRsuId(), keyValue.getRsuId(), "getRsuId");
        assertEquals(key.getIntersectionId(), keyValue.getIntersectionId(), "getIntersectionId");
        assertEquals(key.getRegion(), keyValue.getRegion(), "getRegion");

    }

    @Test
    public void testToString() {
        var key = new RsuIntersectionKey();
        key.setRsuId(ipAddress);
        key.setIntersectionId(intersectionId);

        String str = key.toString();
        assertTrue(str.contains(ipAddress));
        assertTrue(str.contains(Integer.toString(intersectionId)));
    }
}
