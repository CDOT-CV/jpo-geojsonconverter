package us.dot.its.jpo.geojsonconverter.pojos;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class GeometryOutputModeTest {
    @Test
    public void testFindByName() {
        String wktMode = "WKT";
        GeometryOutputMode gomTest = GeometryOutputMode.findByName(wktMode);
        assertNotNull(gomTest);
        assertEquals(GeometryOutputMode.WKT, gomTest);
    }

    @Test
    public void testFindByNameNull() {
        String wktMode = "test";
        GeometryOutputMode gomTest = GeometryOutputMode.findByName(wktMode);
        assertNull(gomTest);
    }
}
