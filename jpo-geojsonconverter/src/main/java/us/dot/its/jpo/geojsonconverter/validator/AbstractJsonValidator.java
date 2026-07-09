package us.dot.its.jpo.geojsonconverter.validator;

import java.io.IOException;
import java.util.List;

import com.networknt.schema.*;
import com.networknt.schema.Error;
import com.networknt.schema.path.PathType;
import lombok.Getter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ResourceLoader;

/**
 * Class for a validator to validate a JSON document against a
 * schema and report all errors.
 */
public abstract class AbstractJsonValidator {

     /**
     * @param schemaLocation The classpath to the schema location
     * For example: ""classpath:schemas/srm.schema.json"
     */
    protected AbstractJsonValidator(String schemaLocation) {
        this.jsonSchemaResource = loadJsonSchemaResource(schemaLocation);
    }

    private final ObjectMapper mapper = new ObjectMapper();
    @Getter
    private final Resource jsonSchemaResource;
    private Schema jsonSchema;


    public Schema getJsonSchema() throws IOException {
        if (jsonSchema == null) {
            try (var inputStream = jsonSchemaResource.getInputStream()) {

                SchemaRegistryConfig config =
                        SchemaRegistryConfig
                                .builder()
                                .cacheRefs(true)
                                .failFast(false)
                                .pathType(PathType.JSON_POINTER)
                                .losslessNarrowing(true)
                                .build();

                SchemaRegistry registry = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2019_09,
                        builder ->
                                builder.schemaRegistryConfig(config)
                                .schemaIdResolvers(resolvers
                                        -> resolvers
                                        // Replace url-based schema id with classpath-based schema id
                                        .mappings(
                                                // Predicate - replace if this is true
                                                source -> source.startsWith("https://") && source.contains("/schemas"),
                                                // Function to transform the schema id
                                                source -> source.replaceFirst("https://.+/schemas", "classpath:/schemas")
                                        )));

                jsonSchema = registry.getSchema(inputStream, InputFormat.JSON);
            } 
        }
        return jsonSchema;
    }

    public JsonValidatorResult validate(String json) {
        var result = new JsonValidatorResult();
        try {
            JsonNode node = mapper.readTree(json);
            List<Error> validationMessages = getJsonSchema().validate(node);
            result.addValidationMessages(validationMessages);
        } catch (Exception e) {
            result.addException(e);
        }
        return result;
    }

    public JsonValidatorResult validate(byte[] jsonBytes) {
        var result = new JsonValidatorResult();
        try { 
            JsonNode node = mapper.readTree(jsonBytes);
            List<Error> validationMessages = getJsonSchema().validate(node);
            result.addValidationMessages(validationMessages);
        } catch (Exception e) {
            result.addException(e);

        }
        return result;
    }

    private Resource loadJsonSchemaResource(String schemaLocation) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        if (schemaLocation != null) {
            return resourceLoader.getResource(schemaLocation);
        }
        return null;
    }
    
}
