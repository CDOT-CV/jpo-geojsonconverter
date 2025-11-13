package us.dot.its.jpo.geojsonconverter.utils;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.BaseFeature;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiPolygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Point;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Custom field resolver that handles the "geometry" field in BaseFeature classes to generate a oneOf schema with all
 * Geometry subtypes.
 */
public class GeometryFieldResolver implements InstanceAttributeOverrideV2<FieldScope> {

    private static final List<Class<? extends Geometry>> GEOMETRY_SUBTYPES =
            List.of(Point.class, LineString.class, Polygon.class, MultiLineString.class, MultiPolygon.class);

    @Override
    public void overrideInstanceAttributes(ObjectNode attributes, FieldScope field, SchemaGenerationContext context) {
        // Only process fields named "geometry"
        if ("geometry".equals(field.getName())) {
            boolean isGeometryType = false;

            // Check if the field's declaring class is BaseFeature or extends it
            // If so, we know the geometry field is always a Geometry type (even if generics are erased)
            Class<?> declaringClass = field.getDeclaringType().getErasedType();
            if (BaseFeature.class.isAssignableFrom(declaringClass)) {
                // This is a geometry field in BaseFeature or a subclass, so it's always Geometry
                isGeometryType = true;
            } else {
                // Get the type from the field's type scope
                TypeScope typeScope = context.getTypeContext().createTypeScope(field.getType());
                Type fieldType = typeScope.getType();
                isGeometryType = checkIfGeometryType(fieldType);
            }

            if (isGeometryType) {
                // Generate a oneOf schema with all Geometry subtypes
                ArrayNode oneOfArray = attributes.putArray("oneOf");

                for (Class<? extends Geometry> geometrySubtype : GEOMETRY_SUBTYPES) {
                    // The schema generator creates definitions with names like "Point-2" for types with type
                    // discriminators
                    // We need to reference the definition that includes the "type" property
                    // Since Geometry types extend GeoJSON which has a "type" property, they get the "-2" suffix
                    String definitionName = geometrySubtype.getSimpleName() + "-2";

                    // Create a reference
                    ObjectNode subtypeSchema = JsonNodeFactory.instance.objectNode();
                    subtypeSchema.put("$ref", "#/$defs/" + definitionName);
                    oneOfArray.add(subtypeSchema);
                }

                // Remove any existing type/format that might have been set
                attributes.remove("type");
                attributes.remove("format");
            }
        }
    }

    private boolean checkIfGeometryType(Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            return Geometry.class.isAssignableFrom(clazz);
        } else if (type instanceof java.lang.reflect.ParameterizedType) {
            java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) type;
            Type rawTypeArg = pt.getRawType();
            if (rawTypeArg instanceof Class) {
                Class<?> clazz = (Class<?>) rawTypeArg;
                return Geometry.class.isAssignableFrom(clazz);
            }
        } else if (type instanceof java.lang.reflect.TypeVariable) {
            // Handle generic type parameters like G extends Geometry
            java.lang.reflect.TypeVariable<?> typeVar = (java.lang.reflect.TypeVariable<?>) type;
            Type[] bounds = typeVar.getBounds();
            for (Type bound : bounds) {
                if (bound instanceof Class && Geometry.class.isAssignableFrom((Class<?>) bound)) {
                    return true;
                } else if (bound instanceof java.lang.reflect.ParameterizedType) {
                    java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) bound;
                    Type rawType = pt.getRawType();
                    if (rawType instanceof Class && Geometry.class.isAssignableFrom((Class<?>) rawType)) {
                        return true;
                    }
                }
            }
        } else if (type instanceof java.lang.reflect.WildcardType) {
            // Handle wildcard types like ? extends Geometry
            java.lang.reflect.WildcardType wildcard = (java.lang.reflect.WildcardType) type;
            Type[] upperBounds = wildcard.getUpperBounds();
            for (Type bound : upperBounds) {
                if (bound instanceof Class && Geometry.class.isAssignableFrom((Class<?>) bound)) {
                    return true;
                }
            }
        }
        return false;
    }
}

