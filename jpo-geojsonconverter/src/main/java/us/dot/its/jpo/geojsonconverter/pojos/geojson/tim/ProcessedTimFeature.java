package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.BaseFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;

/**
 * Represents a GeoJSON Feature for a TIM region.
 * <p>
 * id - The feature ID
 * <p>
 * geometry - The geometry of the TIM region (LineString, Polygon, etc.)
 * <p>
 * properties - The properties of the TIM region
 */
@Generated
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedTimFeature<G extends Geometry> extends BaseFeature<Integer, G, ProcessedTimProperties> {

    @SuppressWarnings("unchecked")
    public ProcessedTimFeature(@JsonProperty("id") Integer id, @JsonProperty("geometry") Geometry geometry,
            @JsonProperty("properties") ProcessedTimProperties properties) {
        super(id, (G) geometry, properties);
    }
}
