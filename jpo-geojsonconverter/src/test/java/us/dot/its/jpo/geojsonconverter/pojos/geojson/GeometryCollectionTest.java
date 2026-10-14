package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GeometryCollectionTest {

    @Test
    void exposesGeometryCollectionTypeAndValueSemantics() {
        Geometry[] geometries = {new Point(-105.0, 40.0), new Point(-104.0, 41.0)};
        GeometryCollection collection = new GeometryCollection(geometries);

        assertEquals("GeometryCollection", collection.getGeoJSONType());
        assertArrayEquals(geometries, collection.getGeometries());
        assertNull(collection.getBbox());
        assertEquals(new GeometryCollection(geometries.clone()), collection);
        assertEquals(collection.hashCode(), new GeometryCollection(geometries.clone()).hashCode());
        assertTrue(collection.toString().contains("geometries"));
        assertNotEquals(collection, new GeometryCollection(new Geometry[] {geometries[0]}));
        assertNotEquals(collection, new GeometryCollection(null));
        assertNotEquals(null, collection);
        assertNotEquals(new Object(), collection);

        GeometryCollection empty = new GeometryCollection(null);
        assertNull(empty.getGeometries());
        assertTrue(empty.toString().contains("null"));
    }
}
