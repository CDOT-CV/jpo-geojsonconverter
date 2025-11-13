package us.dot.its.jpo.geojsonconverter.utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiPolygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Custom type resolver that handles Geometry polymorphic types by generating a oneOf schema with all Geometry subtypes.
 */
public class GeometryTypeResolver implements TypeAttributeOverrideV2 {

    private static final List<Class<? extends Geometry>> GEOMETRY_SUBTYPES =
            List.of(Point.class, LineString.class, Polygon.class, MultiLineString.class, MultiPolygon.class);

    @Override
    public void overrideTypeAttributes(ObjectNode objectNode, TypeScope typeScope, SchemaGenerationContext context) {
        // Get the type from TypeScope - try to get the class
        Type type = typeScope.getType();
        boolean isGeometryType = false;

        // Check if the type is Geometry or extends Geometry
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            isGeometryType = Geometry.class.isAssignableFrom(clazz);
        } else if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
            Type rawTypeArg = pt.getRawType();
            if (rawTypeArg instanceof Class) {
                Class<?> clazz = (Class<?>) rawTypeArg;
                isGeometryType = Geometry.class.isAssignableFrom(clazz);
            }
        } else if (type instanceof java.lang.reflect.TypeVariable) {
            // Handle generic type parameters like G extends Geometry
            java.lang.reflect.TypeVariable<?> typeVar = (java.lang.reflect.TypeVariable<?>) type;
            Type[] bounds = typeVar.getBounds();
            for (Type bound : bounds) {
                if (bound instanceof Class && Geometry.class.isAssignableFrom((Class<?>) bound)) {
                    isGeometryType = true;
                    break;
                } else if (bound instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) bound;
                    Type rawType = pt.getRawType();
                    if (rawType instanceof Class && Geometry.class.isAssignableFrom((Class<?>) rawType)) {
                        isGeometryType = true;
                        break;
                    }
                }
            }
        } else if (type instanceof java.lang.reflect.WildcardType) {
            // Handle wildcard types like ? extends Geometry
            java.lang.reflect.WildcardType wildcard = (java.lang.reflect.WildcardType) type;
            Type[] upperBounds = wildcard.getUpperBounds();
            for (Type bound : upperBounds) {
                if (bound instanceof Class && Geometry.class.isAssignableFrom((Class<?>) bound)) {
                    isGeometryType = true;
                    break;
                }
            }
        }

        if (isGeometryType) {
            // Generate a oneOf schema with all Geometry subtypes
            ArrayNode oneOfArray = objectNode.putArray("oneOf");

            for (Class<? extends Geometry> geometrySubtype : GEOMETRY_SUBTYPES) {
                // Reference the consolidated definition (single definition per type, no suffix)
                String definitionName = geometrySubtype.getSimpleName();

                // Create a reference
                ObjectNode subtypeSchema = JsonNodeFactory.instance.objectNode();
                subtypeSchema.put("$ref", "#/$defs/" + definitionName);
                oneOfArray.add(subtypeSchema);
            }

            // Remove any existing type/format that might have been set
            objectNode.remove("type");
            objectNode.remove("format");
        }
    }
}

