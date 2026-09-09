package us.dot.its.jpo.geojsonconverter.converter.tim;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath.DescriptionChoice;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;
import us.dot.its.jpo.geojsonconverter.pojos.tim.OffsetInformation;
import us.dot.its.jpo.geojsonconverter.pojos.tim.PathNodeData;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.*;
import us.dot.its.jpo.geojsonconverter.converter.FieldConversions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles geometry conversions for TIM regions to GeoJSON types. This class encapsulates all geometry-related
 * conversions and calculations.
 */
@Slf4j
@Component
public class TimGeometryConverter {

    // Constants
    // Adaptive circle point calculation constants
    private static final int MIN_CIRCLE_POINTS = 12;
    private static final int MAX_CIRCLE_POINTS = 64;
    private static final double POINTS_PER_METER = 0.05; // Circle points per meter base calculation: 1 point per 20
                                                         // meters

    /**
     * Calculate the optimal number of points for circle approximation based on diameter. Uses adaptive scaling to
     * balance accuracy with performance.
     * 
     * @param diameterMeters Diameter of the circle in meters
     * @return Number of points to use for circle approximation
     */
    private int calculateAdaptiveCirclePoints(double diameterMeters) {
        // Calculate base number of points based on diameter
        // Larger circles need more points to maintain visual smoothness
        int calculatedPoints = (int) Math.ceil(diameterMeters * POINTS_PER_METER);

        // Apply minimum and maximum bounds
        int adaptivePoints = Math.max(MIN_CIRCLE_POINTS, Math.min(MAX_CIRCLE_POINTS, calculatedPoints));

        // Ensure we have an even number of points for better symmetry
        if (adaptivePoints % 2 != 0) {
            adaptivePoints++;
        }

        log.debug("Adaptive circle points calculation: diameter={}m, calculated={}, final={}", diameterMeters,
                calculatedPoints, adaptivePoints);

        return adaptivePoints;
    }

    /**
     * Convert TIM region to appropriate GeoJSON geometry based on region type.
     *
     * @param region The geographical path region
     * @return Appropriate GeoJSON geometry or null if processing fails
     */
    public Geometry createGeometryFromRegion(GeographicalPath region) {
        try {
            ProcessedRegionType regionType = determineRegionType(region);
            List<List<Double>> coordinates = extractCoordinatesFromRegion(region);

            if (coordinates.isEmpty()) {
                return null;
            }

            return createGeometryByType(coordinates, regionType);
        } catch (Exception e) {
            log.error("Error creating geometry from region: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create geometry from data frame regions.
     *
     * @param dataFrame The data frame containing region information
     * @return Appropriate geometry object or null
     */
    public Geometry createGeometryFromDataFrame(TravelerDataFrame dataFrame) {
        return createGeometryResultFromDataFrame(dataFrame).geometry();
    }

    /**
     * Create geometry for a data frame and retain the mapping from each source region to its emitted geometry
     * component. Regions are converted independently and aggregated in source order.
     *
     * @param dataFrame The data frame containing region information
     * @return Geometry and one component-index entry per source region
     */
    TimGeometryResult createGeometryResultFromDataFrame(TravelerDataFrame dataFrame) {
        if (dataFrame.getRegions() == null || dataFrame.getRegions().isEmpty()) {
            return new TimGeometryResult(null, List.of());
        }

        List<Geometry> geometries = new ArrayList<>();
        List<Integer> regionGeometryIndices = new ArrayList<>(dataFrame.getRegions().size());

        for (int regionIndex = 0; regionIndex < dataFrame.getRegions().size(); regionIndex++) {
            Geometry geometry = createGeometryFromRegion(dataFrame.getRegions().get(regionIndex));
            if (geometry == null) {
                regionGeometryIndices.add(null);
                log.warn("No geometry was produced for TIM region index {}", regionIndex);
                continue;
            }

            regionGeometryIndices.add(geometries.size());
            geometries.add(geometry);
        }

        return new TimGeometryResult(aggregateRegionGeometries(geometries), regionGeometryIndices);
    }

    /**
     * Extract elevation and lane width offset information from a region's path.
     *
     * @param region The geographical path region
     * @return OffsetInformation containing node-aligned elevation and lane width offsets, or null if no path
     */
    public OffsetInformation extractOffsetInformation(GeographicalPath region) {
        if (region.getDescription() == null || region.getDescription().getPath() == null) {
            return null;
        }

        List<PathNodeData> pathData = processOffsetPathWithOffsets(region, region.getDescription().getPath());
        if (pathData.isEmpty()) {
            return null;
        }

        List<Long> elevationOffsets = new ArrayList<>(pathData.size());
        List<Long> laneWidthOffsets = new ArrayList<>(pathData.size());
        boolean hasElevationOffset = false;
        boolean hasLaneWidthOffset = false;

        for (PathNodeData nodeData : pathData) {
            Long elevationOffset = nodeData.getDelevationOffset();
            Long laneWidthOffset = nodeData.getDwidthOffset();
            elevationOffsets.add(elevationOffset);
            laneWidthOffsets.add(laneWidthOffset);
            hasElevationOffset |= elevationOffset != null;
            hasLaneWidthOffset |= laneWidthOffset != null;
        }

        return new OffsetInformation(hasElevationOffset ? elevationOffsets : null,
                hasLaneWidthOffset ? laneWidthOffsets : null);
    }

    /**
     * Calculate a representative location from all valid region anchors in the TIM message.
     *
     * <p>
     * This point is intended for coarse geospatial indexing. It is not the centroid of the complete TIM geometry, and
     * callers should use the feature geometries for intersection or roadway-traversal queries. Longitudes are
     * unwrapped around the first anchor before averaging so anchors that straddle the antimeridian stay near ±180
     * rather than averaging to 0.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @return JTS Point representing the average region-anchor location, or {@code null} when no valid anchors are
     *         available
     */
    public Point calculateCenterLocationFromRegionAnchors(TravelerInformation travelerInfo) {
        List<Double> longitudes = new ArrayList<>();
        List<Double> latitudes = new ArrayList<>();

        if (travelerInfo.getDataFrames() != null) {
            for (TravelerDataFrame dataFrame : travelerInfo.getDataFrames()) {
                if (dataFrame.getRegions() != null) {
                    for (GeographicalPath region : dataFrame.getRegions()) {
                        // Get anchor point coordinates
                        if (region.getAnchor() != null && region.getAnchor().getLat() != null
                                && region.getAnchor().getLong_() != null) {
                            Double lat = FieldConversions.convertLat(region.getAnchor().getLat().getValue());
                            Double lon = FieldConversions.convertLong(region.getAnchor().getLong_().getValue());
                            if (lat != null && lon != null) {
                                longitudes.add(lon);
                                latitudes.add(lat);
                            }
                        }
                    }
                }
            }
        }

        if (longitudes.isEmpty()) {
            log.warn("Cannot calculate a representative TIM location: no valid region anchors were found");
            return null;
        }

        // Create Point geometry using JTS GeometryFactory
        return new GeometryFactory()
                .createPoint(new Coordinate(averageLongitude(longitudes), average(latitudes)));
    }

    /**
     * Arithmetic mean of longitudes after unwrapping around the first value. Nearby longitudes match a simple average;
     * values such as 179° and -179° stay on the antimeridian instead of averaging to 0°.
     *
     * @param longitudes Longitude values in decimal degrees
     * @return Mean longitude in the range [-180, 180]
     */
    static double averageLongitude(List<Double> longitudes) {
        double reference = longitudes.get(0);
        double[] unwrapped = new double[longitudes.size()];
        for (int i = 0; i < longitudes.size(); i++) {
            unwrapped[i] = unwrapLongitude(longitudes.get(i), reference);
        }
        return wrapLongitude(Arrays.stream(unwrapped).average().getAsDouble());
    }

    static double unwrapLongitude(double longitude, double reference) {
        double delta = longitude - reference;
        if (delta > 180.0) {
            delta -= 360.0;
        } else if (delta < -180.0) {
            delta += 360.0;
        }
        return reference + delta;
    }

    static double wrapLongitude(double longitude) {
        if (longitude > 180.0) {
            return longitude - 360.0;
        }
        if (longitude < -180.0) {
            return longitude + 360.0;
        }
        return longitude;
    }

    private static double average(List<Double> values) {
        double[] array = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return Arrays.stream(array).average().getAsDouble();
    }

    /**
     * Determine the region type from ASN.1 data.
     */
    ProcessedRegionType determineRegionType(GeographicalPath region) {
        if (region.getDescription() == null) {
            return ProcessedRegionType.UNKNOWN;
        }

        boolean isClosedPath = region.getClosedPath() != null && region.getClosedPath().getValue();
        boolean isCircle = region.getDescription().getGeometry() != null
                && region.getDescription().getGeometry().getCircle() != null;
        boolean hasPath = region.getDescription().getPath() != null;

        if (isCircle) {
            return ProcessedRegionType.CIRCLE;
        } else if (isClosedPath) {
            return ProcessedRegionType.POLYGON;
        } else if (hasPath) {
            return ProcessedRegionType.PATH;
        } else {
            return ProcessedRegionType.UNKNOWN;
        }
    }

    /**
     * Extract coordinates from a TIM region
     */
    private List<List<Double>> extractCoordinatesFromRegion(GeographicalPath region) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Get coordinates from the description
        DescriptionChoice description = region.getDescription();
        if (description != null) {
            if (description.getPath() != null) {
                coordinates.addAll(extractCoordinatesFromOffsetPath(region, description.getPath()));
            } else if (description.getGeometry() != null) {
                coordinates.addAll(extractCoordinatesFromGeometry(region, description.getGeometry()));
            }
        }

        if (coordinates.isEmpty() && region.getAnchor() != null) {
            log.warn("No coordinates found for region: {}", region.getAnchor());
        }

        return coordinates;
    }

    /**
     * Create geometry based on the region type.
     */
    private Geometry createGeometryByType(List<List<Double>> coordinates, ProcessedRegionType regionType) {
        return switch (regionType) {
            case CIRCLE, POLYGON -> createPolygonFromCoordinates(coordinates);
            case PATH, UNKNOWN -> createLineStringFromCoordinates(coordinates);
        };
    }

    /** Aggregate already-converted region geometries without changing source order. */
    private Geometry aggregateRegionGeometries(List<Geometry> geometries) {
        if (geometries.isEmpty()) {
            return null;
        }
        if (geometries.size() == 1) {
            return geometries.getFirst();
        }

        if (geometries.stream().allMatch(LineString.class::isInstance)) {
            double[][][] coordinates = new double[geometries.size()][][];
            for (int i = 0; i < geometries.size(); i++) {
                coordinates[i] = ((LineString) geometries.get(i)).getCoordinates();
            }
            return new MultiLineString(coordinates);
        }

        if (geometries.stream().allMatch(Polygon.class::isInstance)) {
            double[][][][] coordinates = new double[geometries.size()][][][];
            for (int i = 0; i < geometries.size(); i++) {
                coordinates[i] = ((Polygon) geometries.get(i)).getCoordinates();
            }
            return new MultiPolygon(coordinates);
        }

        return new GeometryCollection(geometries.toArray(new Geometry[0]));
    }

    /**
     * Calculate zoom factor from path scale.
     * 
     * @param path The OffsetSystem path
     * @return Zoom factor (2^zoom)
     */
    private double calculateZoomFactor(OffsetSystem path) {
        if (path.getScale() != null) {
            // Zoom is applied as 2^zoom for coordinate scaling
            // A value of 0 is 1:1 zoom (no zoom), 1 is 2:1 zoom, 2 is 4:1 zoom, etc.
            return Math.pow(2, path.getScale().getValue());
        }
        return 1.0;
    }

    /**
     * Extract dwidth and delevation offsets from node attributes.
     * 
     * @param node The node to extract offsets from
     * @return Array containing [dwidthOffset, delevationOffset] or null if no attributes
     */
    private Long[] extractNodeOffsets(NodeLL node) {
        if (node.getAttributes() != null) {
            Long dwidthOffset = node.getAttributes().getDWidth() != null
                    ? node.getAttributes().getDWidth().getValue()
                    : null;
            Long delevationOffset = node.getAttributes().getDElevation() != null
                    ? node.getAttributes().getDElevation().getValue()
                    : null;

            return new Long[] {dwidthOffset, delevationOffset};
        }
        return null;
    }

    /**
     * Extract dwidth and delevation offsets from node attributes.
     * 
     * @param node The node to extract offsets from
     * @return Array containing [dwidthOffset, delevationOffset] or null if no attributes
     */
    private Long[] extractNodeOffsets(NodeXY node) {
        if (node.getAttributes() != null) {
            Long dwidthOffset = node.getAttributes().getDWidth() != null
                    ? node.getAttributes().getDWidth().getValue()
                    : null;
            Long delevationOffset = node.getAttributes().getDElevation() != null
                    ? node.getAttributes().getDElevation().getValue()
                    : null;

            return new Long[] {dwidthOffset, delevationOffset};
        }
        return null;
    }

    /**
     * Process J2735 NodeOffsetPointLL (Latitude/Longitude) node and update current coordinates with calculated latitude
     * and longitude of the node.
     * 
     * @param node The node to process
     * @param zoomFactor Zoom scaling factor
     * @param currentCoords Current coordinates [lon, lat] to update. For LatLon nodes (absolute coordinates), this may
     *        be uninitialized or contain default values.
     */
    private void processLLNode(NodeOffsetPointLL node, double zoomFactor, double[] currentCoords) {
        // Initialize with current coordinates if available, otherwise use defaults
        double currentLon = (currentCoords != null && currentCoords.length > 0) ? currentCoords[0] : 0.0;
        double currentLat = (currentCoords != null && currentCoords.length > 1) ? currentCoords[1] : 0.0;

        // Process different LL node types
        if (node.getNode_LL1() != null) {
            var nodeLL1 = node.getNode_LL1();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL1.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL1.getLat().getValue(), zoomFactor);
            if (lonOffset != null) currentLon += lonOffset;
            if (latOffset != null) currentLat += latOffset;
        } else if (node.getNode_LL2() != null) {
            var nodeLL2 = node.getNode_LL2();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL2.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL2.getLat().getValue(), zoomFactor);
            if (lonOffset != null) currentLon += lonOffset;
            if (latOffset != null) currentLat += latOffset;
        } else if (node.getNode_LL3() != null) {
            var nodeLL3 = node.getNode_LL3();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL3.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL3.getLat().getValue(), zoomFactor);
            if (lonOffset != null) currentLon += lonOffset;
            if (latOffset != null) currentLat += latOffset;
        } else if (node.getNode_LL4() != null) {
            var nodeLL4 = node.getNode_LL4();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL4.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL4.getLat().getValue(), zoomFactor);
            if (lonOffset != null) currentLon += lonOffset;
            if (latOffset != null) currentLat += latOffset;
        } else if (node.getNode_LL5() != null) {
            var nodeLL5 = node.getNode_LL5();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL5.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL5.getLat().getValue(), zoomFactor);
            if (lonOffset != null) currentLon += lonOffset;
            if (latOffset != null) currentLat += latOffset;
        } else if (node.getNode_LL6() != null) {
            var nodeLL6 = node.getNode_LL6();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL6.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL6.getLat().getValue(), zoomFactor);
            if (lonOffset != null) currentLon += lonOffset;
            if (latOffset != null) currentLat += latOffset;
        } else if (node.getNode_LatLon() != null) {
            var nodeLatLon = node.getNode_LatLon();
            // node_LatLon contains absolute coordinates, not offsets - doesn't require anchor
            Double absLon = FieldConversions.convertLong(nodeLatLon.getLon().getValue());
            Double absLat = FieldConversions.convertLat(nodeLatLon.getLat().getValue());
            if (absLon != null && absLat != null) {
                currentLon = absLon;
                currentLat = absLat;
            }
        }

        // Update coordinates array (ensure it's initialized)
        if (currentCoords == null || currentCoords.length < 2) {
            // This shouldn't happen in normal flow, but handle defensively
            return;
        }
        currentCoords[0] = currentLon;
        currentCoords[1] = currentLat;
    }

    /**
     * Process an XY node and update the current coordinates. Relative XY node variants require a current reference
     * point; {@code node-LatLon} supplies an absolute point and establishes that reference itself.
     * 
     * @param node The node to process
     * @param zoomFactor Zoom scaling factor
     * @param currentCoords Current coordinates [lon, lat] to update
     */
    private void processXYNode(NodeOffsetPointXY node, double zoomFactor, double[] currentCoords) {
        // The coordinate array also carries an absolute node-LatLon value back to the caller.
        if (currentCoords == null || currentCoords.length < 2) {
            log.warn("Cannot process XY node: currentCoords is null or invalid");
            return;
        }

        double currentLon = currentCoords[0];
        double currentLat = currentCoords[1];

        // Process different XY node types
        if (node.getNode_XY1() != null) {
            var nodeXY1 = node.getNode_XY1();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY1.getX().getValue(), nodeXY1.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY2() != null) {
            var nodeXY2 = node.getNode_XY2();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY2.getX().getValue(), nodeXY2.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY3() != null) {
            var nodeXY3 = node.getNode_XY3();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY3.getX().getValue(), nodeXY3.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY4() != null) {
            var nodeXY4 = node.getNode_XY4();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY4.getX().getValue(), nodeXY4.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY5() != null) {
            var nodeXY5 = node.getNode_XY5();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY5.getX().getValue(), nodeXY5.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_XY6() != null) {
            var nodeXY6 = node.getNode_XY6();
            double[] offsets = FieldConversions.convertJ2735XY(nodeXY6.getX().getValue(), nodeXY6.getY().getValue(),
                    currentLat, zoomFactor);
            currentLon += offsets[0];
            currentLat += offsets[1];
        } else if (node.getNode_LatLon() != null) {
            var nodeLatLon = node.getNode_LatLon();
            Double absoluteLon = FieldConversions.convertLong(nodeLatLon.getLon().getValue());
            Double absoluteLat = FieldConversions.convertLat(nodeLatLon.getLat().getValue());
            if (absoluteLon != null && absoluteLat != null) {
                currentLon = absoluteLon;
                currentLat = absoluteLat;
            }
        }

        // Update coordinates array
        currentCoords[0] = currentLon;
        currentCoords[1] = currentLat;
    }

    /**
     * Process offset path nodes and return their coordinates and offset information.
     * 
     * @param region The geographical path region
     * @param path The offset system path
     * @return List of PathNodeData containing coordinates and offset information
     */
    private List<PathNodeData> processOffsetPathWithOffsets(GeographicalPath region, OffsetSystem path) {
        if (path == null || path.getOffset() == null) {
            return new ArrayList<>();
        }

        Double anchorLat = null;
        Double anchorLon = null;
        if (region.getAnchor() != null && region.getAnchor().getLat() != null
                && region.getAnchor().getLong_() != null) {
            anchorLat = FieldConversions.convertLat(region.getAnchor().getLat().getValue());
            anchorLon = FieldConversions.convertLong(region.getAnchor().getLong_().getValue());
        }

        List<PathNodeData> pathData = new ArrayList<>();
        boolean hasAnchor = anchorLat != null && anchorLon != null;
        double[] currentCoords = hasAnchor ? new double[] {anchorLon, anchorLat} : new double[2];
        boolean hasCurrentCoordinates = hasAnchor;
        boolean missingReferenceLogged = false;
        double zoomFactor = calculateZoomFactor(path);

        // Handle LL (Latitude/Longitude) coordinates
        if (path.getOffset().getLl() != null && path.getOffset().getLl().getNodes() != null) {
            for (var node : path.getOffset().getLl().getNodes()) {
                if (node.getDelta() != null) {
                    boolean isAbsoluteNode = node.getDelta().getNode_LatLon() != null;
                    if (hasCurrentCoordinates || isAbsoluteNode) {
                        processLLNode(node.getDelta(), zoomFactor, currentCoords);
                        hasCurrentCoordinates = true;
                        Long[] offsets = extractNodeOffsets(node);
                        Long dwidthOffset = offsets != null ? offsets[0] : null;
                        Long delevationOffset = offsets != null ? offsets[1] : null;
                        pathData.add(new PathNodeData(Arrays.asList(currentCoords[0], currentCoords[1]), dwidthOffset,
                                delevationOffset));
                    } else if (!missingReferenceLogged) {
                        log.warn(
                                "Skipping TIM LL offset attributes because the region has no anchor or preceding absolute LatLon node");
                        missingReferenceLogged = true;
                    }
                }
            }
        }
        // Handle XY (Cartesian) coordinates
        else if (path.getOffset().getXy() != null && path.getOffset().getXy().getNodes() != null) {
            for (var node : path.getOffset().getXy().getNodes()) {
                if (node.getDelta() != null) {
                    boolean isAbsoluteNode = node.getDelta().getNode_LatLon() != null;
                    if (hasCurrentCoordinates || isAbsoluteNode) {
                        processXYNode(node.getDelta(), zoomFactor, currentCoords);
                        hasCurrentCoordinates = true;
                        Long[] offsets = extractNodeOffsets(node);
                        Long dwidthOffset = offsets != null ? offsets[0] : null;
                        Long delevationOffset = offsets != null ? offsets[1] : null;
                        pathData.add(new PathNodeData(Arrays.asList(currentCoords[0], currentCoords[1]), dwidthOffset,
                                delevationOffset));
                    } else if (!missingReferenceLogged) {
                        log.warn(
                                "Skipping TIM XY offset attributes because the region has no anchor or preceding absolute LatLon node");
                        missingReferenceLogged = true;
                    }
                }
            }
        }

        return pathData;
    }

    private List<List<Double>> extractCoordinatesFromOffsetPath(GeographicalPath region, OffsetSystem path) {
        if (path == null) {
            return new ArrayList<>();
        }

        List<List<Double>> coordinates = new ArrayList<>();

        // Initialize anchor coordinates if available
        Double anchorLat = null;
        Double anchorLon = null;
        boolean hasAnchor = false;

        if (region.getAnchor() != null && region.getAnchor().getLat() != null
                && region.getAnchor().getLong_() != null) {
            Position3D anchor = region.getAnchor();
            anchorLat = FieldConversions.convertLat(anchor.getLat().getValue());
            anchorLon = FieldConversions.convertLong(anchor.getLong_().getValue());
            hasAnchor = anchorLat != null && anchorLon != null;
        }

        if (path.getOffset() != null) {
            double zoomFactor = calculateZoomFactor(path);

            // Handle LL (Latitude/Longitude) coordinates
            if (path.getOffset().getLl() != null && path.getOffset().getLl().getNodes() != null) {
                double[] currentCoords = hasAnchor ? new double[] {anchorLon, anchorLat} : new double[2];
                boolean hasCurrentCoordinates = hasAnchor;
                boolean missingAnchorLogged = false;

                for (var node : path.getOffset().getLl().getNodes()) {
                    if (node.getDelta() != null) {
                        // Check if this is a LatLon node (absolute coordinates) that doesn't need anchor
                        boolean isLatLonNode = node.getDelta().getNode_LatLon() != null;

                        // An absolute LatLon node establishes a starting coordinate for any following offsets.
                        if (hasCurrentCoordinates || isLatLonNode) {
                            processLLNode(node.getDelta(), zoomFactor, currentCoords);
                            hasCurrentCoordinates = true;
                            coordinates.add(Arrays.asList(currentCoords[0], currentCoords[1]));
                        } else if (!missingAnchorLogged) {
                            log.warn(
                                    "Skipping TIM LL offset nodes because the region has no anchor or preceding absolute LatLon node");
                            missingAnchorLogged = true;
                        }
                    }
                }
            }
            // Handle XY coordinates. Absolute LatLon nodes may establish a path without an anchor.
            else if (path.getOffset().getXy() != null && path.getOffset().getXy().getNodes() != null) {
                double[] currentCoords = hasAnchor ? new double[] {anchorLon, anchorLat} : new double[2];
                boolean hasCurrentCoordinates = hasAnchor;
                boolean missingAnchorLogged = false;

                for (var node : path.getOffset().getXy().getNodes()) {
                    if (node.getDelta() != null) {
                        boolean isLatLonNode = node.getDelta().getNode_LatLon() != null;
                        if (hasCurrentCoordinates || isLatLonNode) {
                            processXYNode(node.getDelta(), zoomFactor, currentCoords);
                            hasCurrentCoordinates = true;
                            coordinates.add(Arrays.asList(currentCoords[0], currentCoords[1]));
                        } else if (!missingAnchorLogged) {
                            log.warn(
                                    "Skipping TIM XY offset nodes because the region has no anchor or preceding absolute LatLon node");
                            missingAnchorLogged = true;
                        }
                    }
                }
            }
        }

        return coordinates;
    }

    private List<List<Double>> extractCoordinatesFromGeometry(GeographicalPath region, GeometricProjection geometry) {
        if (geometry == null) {
            return new ArrayList<>();
        }

        List<List<Double>> coordinates = new ArrayList<>();

        // Handle circle geometry
        if (geometry.getCircle() != null) {
            Circle circle = geometry.getCircle();
            if (circle.getRadius() != null && circle.getCenter() != null) {
                long radius = circle.getRadius().getValue();
                DistanceUnits units = circle.getUnits();
                Double radiusMeters = FieldConversions.convertDistanceToMeters(radius, units);

                // Use circle's center coordinates, not the anchor point
                Double centerLat = FieldConversions.convertLat(circle.getCenter().getLat().getValue());
                Double centerLon = FieldConversions.convertLong(circle.getCenter().getLong_().getValue());

                if (radiusMeters != null && centerLat != null && centerLon != null) {
                    // Create circle points using accurate geodetic calculations
                    coordinates.addAll(createCirclePoints(centerLon, centerLat, radiusMeters));
                } else {
                    log.error("Invalid circle radius or center: radius={}, centerLat={}, centerLon={}", radius,
                            centerLat, centerLon);
                }
            } else {
                log.warn("Circle geometry missing radius or center field");
            }
        }

        return coordinates;
    }

    /**
     * Create circle points geodesically on WGS84. This avoids UTM zone-boundary and polar distortion while preserving
     * the source radius at every generated vertex.
     * 
     * @param centerLon Center longitude in degrees
     * @param centerLat Center latitude in degrees
     * @param radiusMeters Radius in meters
     * @return List of coordinate points forming a circle
     */
    private List<List<Double>> createCirclePoints(double centerLon, double centerLat, double radiusMeters) {
        List<List<Double>> coordinates = new ArrayList<>();

        if (!Double.isFinite(centerLon) || !Double.isFinite(centerLat) || !Double.isFinite(radiusMeters)
                || centerLon < -180.0 || centerLon > 180.0 || centerLat < -90.0 || centerLat > 90.0
                || radiusMeters < 0.0) {
            log.error("Invalid circle parameters: center=({}, {}), radius={}m", centerLon, centerLat, radiusMeters);
            return coordinates;
        }

        try {
            double diameterMeters = radiusMeters * 2.0;
            int adaptivePoints = calculateAdaptiveCirclePoints(diameterMeters);
            GeodeticCalculator calculator = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
            calculator.setStartingGeographicPoint(centerLon, centerLat);

            // Start east of the center and proceed counter-clockwise to retain GeoJSON exterior-ring orientation.
            for (int pointIndex = 0; pointIndex < adaptivePoints; pointIndex++) {
                double azimuthDegrees = 90.0 - (360.0 * pointIndex / adaptivePoints);
                calculator.setDirection(azimuthDegrees, radiusMeters);
                Point2D destination = calculator.getDestinationGeographicPoint();
                double longitude = normalizeLongitude(destination.getX());
                double latitude = destination.getY();
                if (!Double.isFinite(longitude) || !Double.isFinite(latitude)) {
                    log.error("Geodesic circle calculation produced a non-finite coordinate");
                    return new ArrayList<>();
                }
                coordinates.add(Arrays.asList(longitude, latitude));
            }

            // GeoJSON polygon rings repeat the first point as the final point.
            coordinates.add(new ArrayList<>(coordinates.getFirst()));

            log.debug("Created geodesic circle with {} vertices, center=({}, {}), radius={}m", adaptivePoints,
                    centerLon, centerLat, radiusMeters);
        } catch (Exception e) {
            log.error("Error creating geodesic circle: {}", e.getMessage(), e);
            coordinates.clear();
        }

        return coordinates;
    }

    private double normalizeLongitude(double longitude) {
        double normalized = longitude % 360.0;
        if (normalized > 180.0) {
            normalized -= 360.0;
        } else if (normalized < -180.0) {
            normalized += 360.0;
        }
        return normalized;
    }

    private LineString createLineStringFromCoordinates(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }

        double[][] coordinateArray = new double[coordinates.size()][2];
        for (int i = 0; i < coordinates.size(); i++) {
            List<Double> coord = coordinates.get(i);
            if (coord.size() >= 2) {
                coordinateArray[i][0] = coord.get(0); // longitude
                coordinateArray[i][1] = coord.get(1); // latitude
            }
        }

        return new LineString(coordinateArray);
    }

    private Polygon createPolygonFromCoordinates(List<List<Double>> coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return null;
        }

        // Ensure the polygon is closed (first and last coordinates are the same)
        List<List<Double>> closedCoordinates = new ArrayList<>(coordinates);
        if (closedCoordinates.size() > 2) {
            List<Double> first = closedCoordinates.get(0);
            List<Double> last = closedCoordinates.get(closedCoordinates.size() - 1);
            if (!first.equals(last)) {
                closedCoordinates.add(new ArrayList<>(first));
            }
        }

        double[][][] coordinateArray = new double[1][closedCoordinates.size()][2];
        for (int i = 0; i < closedCoordinates.size(); i++) {
            List<Double> coord = closedCoordinates.get(i);
            if (coord.size() >= 2) {
                coordinateArray[0][i][0] = coord.get(0); // longitude
                coordinateArray[0][i][1] = coord.get(1); // latitude
            }
        }

        return new Polygon(coordinateArray);
    }

}
