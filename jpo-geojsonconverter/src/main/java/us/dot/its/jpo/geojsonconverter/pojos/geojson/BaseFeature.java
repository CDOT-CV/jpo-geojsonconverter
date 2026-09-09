package us.dot.its.jpo.geojsonconverter.pojos.geojson;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import us.dot.its.jpo.geojsonconverter.DateJsonMapper;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@JsonIgnoreProperties(value = {"type"}, allowGetters = true)
@JsonPropertyOrder({"type", "id", "geometry", "properties"})
@Getter
@Slf4j
public abstract class BaseFeature<TId, TGeometry, TProperties> {

    @JsonInclude(Include.NON_EMPTY)
    protected final TId id;
    protected final TGeometry geometry;
    protected final TProperties properties;

    @JsonCreator
    public BaseFeature(@JsonProperty("id") TId id, @JsonProperty("geometry") TGeometry geometry,
            @JsonProperty("properties") TProperties properties) {
        this.id = id;
        this.geometry = geometry;
        this.properties = properties;
    }

    @JsonProperty("type")
    public String getType() {
        return "Feature";
    }

    @Override
    public String toString() {
        ObjectMapper mapper = DateJsonMapper.getInstance();
        String testReturn = "";
        try {
            testReturn = (mapper.writeValueAsString(this));
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);
        }
        return testReturn;
    }
}
