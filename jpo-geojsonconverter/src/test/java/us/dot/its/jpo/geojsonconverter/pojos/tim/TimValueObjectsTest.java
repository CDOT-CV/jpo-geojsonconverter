package us.dot.its.jpo.geojsonconverter.pojos.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class TimValueObjectsTest {

    @Test
    void pathNodeDataExposesAndComparesEveryValue() {
        PathNodeData value = new PathNodeData(List.of(-105.0, 40.0), 12L, 34L);
        PathNodeData equalValue = new PathNodeData(List.of(-105.0, 40.0), 12L, 34L);

        assertEquals(List.of(-105.0, 40.0), value.getCoordinates());
        assertEquals(12L, value.getDwidthOffset());
        assertEquals(34L, value.getDelevationOffset());
        assertEquals(value, equalValue);
        assertEquals(equalValue, value);
        assertEquals(value.hashCode(), equalValue.hashCode());
        assertTrue(value.toString().contains("coordinates"));
        assertNotEquals(value, new PathNodeData(List.of(-104.0, 40.0), 12L, 34L));
        assertNotEquals(value, new PathNodeData(List.of(-105.0, 40.0), 13L, 34L));
        assertNotEquals(value, new PathNodeData(List.of(-105.0, 40.0), 12L, 35L));
        assertNotEquals(null, value);
        assertNotEquals(value, new Object());

        PathNodeData nullValues = new PathNodeData(null, null, null);
        assertEquals(nullValues, new PathNodeData(null, null, null));
        assertNotEquals(value, nullValues);
        assertEquals(nullValues.hashCode(), new PathNodeData(null, null, null).hashCode());
        assertTrue(nullValues.toString().contains("null"));
    }

    @Test
    void offsetInformationExposesAndComparesBothOffsetLists() {
        OffsetInformation value = new OffsetInformation(Arrays.asList(1L, null), List.of(2L, 3L));
        OffsetInformation equalValue = new OffsetInformation(Arrays.asList(1L, null), List.of(2L, 3L));

        assertEquals(Arrays.asList(1L, null), value.getElevationOffsets());
        assertEquals(List.of(2L, 3L), value.getLaneWidthOffsets());
        assertEquals(value, equalValue);
        assertEquals(value.hashCode(), equalValue.hashCode());
        assertTrue(value.toString().contains("elevationOffsets"));
        assertNotEquals(value, new OffsetInformation(List.of(4L), List.of(2L, 3L)));
        assertNotEquals(value, new OffsetInformation(Arrays.asList(1L, null), List.of(5L)));
        assertNotEquals(value, null);
        assertNotEquals(value, new Object());

        OffsetInformation nullValues = new OffsetInformation(null, null);
        assertEquals(nullValues, new OffsetInformation(null, null));
        assertNotEquals(value, nullValues);
        assertTrue(nullValues.toString().contains("null"));
    }
}
