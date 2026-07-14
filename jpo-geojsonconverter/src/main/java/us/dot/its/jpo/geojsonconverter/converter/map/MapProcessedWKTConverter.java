package us.dot.its.jpo.geojsonconverter.converter.map;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.KeyValueMapper;

import us.dot.its.jpo.geojsonconverter.converter.WKTHandler;
import us.dot.its.jpo.geojsonconverter.partitioner.RsuIntersectionKey;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.map.ProcessedMap;

public class MapProcessedWKTConverter implements KeyValueMapper<RsuIntersectionKey, ProcessedMap<LineString>, KeyValue<RsuIntersectionKey, ProcessedMap<String>>> {

    /**
     * Apply the conversion from a ProcessedMap GeoJSON POJO to ProcessedMap WKT POJO.
     * 
     * @param key   - {@link RsuIntersectionKey} containing the RSU IP address and Intersection ID
     * @param geoJSONMap - GeoJSON FeatureCollection POJO
     * @return A key value pair: the key an {@link RsuIntersectionKey} containing the RSU IP address and Intersection ID
     *  and the value is the WKT FeatureCollection POJO
     */
    @Override
    public KeyValue<RsuIntersectionKey, ProcessedMap<String>> apply(RsuIntersectionKey key, ProcessedMap<LineString> geoJSONMap) {
        ProcessedMap<String> wktProcessedMap = WKTHandler.processedMapGeoJSON2WKT(geoJSONMap);

        // Remove parts of the ProcessedMap that are not needed since WKT is not used by the Conflict Monitor
        wktProcessedMap.getProperties().setValidationMessages(null);
        for (int i = 0; i < wktProcessedMap.getMapFeatureCollection().getFeatures().length; i++) {
            wktProcessedMap.getMapFeatureCollection().getFeatures()[i].getProperties().setNodes(null);
        }

        return KeyValue.pair(key, wktProcessedMap);
    }

}
