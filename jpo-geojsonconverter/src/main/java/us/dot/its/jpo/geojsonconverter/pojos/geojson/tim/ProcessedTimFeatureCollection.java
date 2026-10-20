package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.GeoJSON;

/**
 * Represents a GeoJSON FeatureCollection for TIM data frames.
 * <p>
 * type - Always "FeatureCollection" for GeoJSON FeatureCollection
 * <p>
 * features - List of TIM data frame features
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@lombok.EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedTimFeatureCollection extends GeoJSON {
    private List<ProcessedTimFeature<?>> features;

    @Override
    protected String getGeoJSONType() {
        return "FeatureCollection";
    }
}
