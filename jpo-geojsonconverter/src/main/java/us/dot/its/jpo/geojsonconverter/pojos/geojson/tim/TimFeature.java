package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.*;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.BaseFeature;

public class TimFeature<TGeometry> extends BaseFeature<Integer, TGeometry, TimProperties> {
    @JsonCreator
    public TimFeature(
            @JsonProperty("id") Integer id,
            @JsonProperty("geometry") TGeometry geometry, 
            @JsonProperty("properties") TimProperties properties) {
        super(id, geometry, properties);
    }
}
