package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@JsonInclude(Include.NON_NULL)
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class MultiLineString extends Geometry {
    private final double[][][] coordinates;
    private final double[] bbox;

    @JsonCreator
    public MultiLineString(@JsonProperty("coordinates") double[][][] coordinates) {
        super();
        this.coordinates = coordinates;
        this.bbox = null;
    }
}
