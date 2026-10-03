package us.dot.its.jpo.geojsonconverter.partitioner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.IntersectionID;
import us.dot.its.jpo.asn.j2735.r2024.Common.IntersectionReferenceID;
import us.dot.its.jpo.asn.j2735.r2024.Common.RoadRegulatorID;


public class RsuIntersectionKeyTest {

    static final String IP_ADDRESS = "127.0.0.1";
    static final int INTERSECTION_ID = 10001;
    static final int REGION = 10;

    @Test
    public void testEquality() {

        var key = new RsuIntersectionKey();
        key.setRsuId(IP_ADDRESS);
        var intersectionRegion = new IntersectionReferenceID();
        intersectionRegion.setId(new IntersectionID(INTERSECTION_ID));
        intersectionRegion.setRegion(new RoadRegulatorID(REGION));
        key.setIntersectionReferenceID(intersectionRegion);

        var keyValue = new RsuIntersectionKey(IP_ADDRESS, INTERSECTION_ID, REGION);
        var keyRef = key;
        Object otherObject = new Object();
        var otherValue1 = new RsuIntersectionKey(IP_ADDRESS, 99);
        var otherValue2 = new RsuIntersectionKey("0.0.0.0", 99);
        var otherValue3 = new RsuIntersectionKey(IP_ADDRESS, INTERSECTION_ID, 99);

        assertEquals(keyValue, key, "Value equality");
        assertNotEquals(otherValue1, key, "Value inequality branch 1");
        assertNotEquals(otherValue2, key, "Value inequality branch 2");
        assertNotEquals(otherValue3, key, "Value inequality branch 3");
        assertTrue(key.equals(keyRef), "Reference equality");
        assertNotEquals(otherObject, key, "Reference inequality");
        assertEquals(key.hashCode(), keyValue.hashCode(), "Hash code values equal");

        // Getter coverage
        assertEquals(key.getRsuId(), keyValue.getRsuId(), "getRsuId");
        assertEquals(key.getIntersectionId(), keyValue.getIntersectionId(), "getIntersectionId");
        assertEquals(key.getRegion(), keyValue.getRegion(), "getRegion");

    }

    @Test
    public void testToString() {
        var key = new RsuIntersectionKey();
        key.setRsuId(IP_ADDRESS);
        key.setIntersectionId(INTERSECTION_ID);

        String str = key.toString();
        assertTrue(str.contains(IP_ADDRESS));
        assertTrue(str.contains(Integer.toString(INTERSECTION_ID)));
    }
}
