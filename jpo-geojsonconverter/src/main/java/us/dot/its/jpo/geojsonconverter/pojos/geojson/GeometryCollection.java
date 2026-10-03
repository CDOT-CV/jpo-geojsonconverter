package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * GeoJSON GeometryCollection used when a TIM data frame contains mixed region types
 * (e.g. path + circle), as shown in ITWG TIM best-practice examples.
 */
@JsonInclude(Include.NON_NULL)
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class GeometryCollection extends Geometry {
    private final Geometry[] geometries;
    private final double[] bbox;

    @JsonCreator
    public GeometryCollection(@JsonProperty("geometries") Geometry[] geometries) {
        super();
        this.geometries = geometries;
        this.bbox = null;
    }
}
