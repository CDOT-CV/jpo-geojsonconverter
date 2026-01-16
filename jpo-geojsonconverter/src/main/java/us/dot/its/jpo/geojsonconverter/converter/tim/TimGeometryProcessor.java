package us.dot.its.jpo.geojsonconverter.converter.tim;

import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath.DescriptionChoice;
import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;
import us.dot.its.jpo.geojsonconverter.pojos.tim.OffsetInformation;
import us.dot.its.jpo.geojsonconverter.pojos.tim.PathNodeData;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.*;
import us.dot.its.jpo.geojsonconverter.converter.FieldConversions;
import us.dot.its.jpo.geojsonconverter.utils.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.util.GeometricShapeFactory;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Handles geometry processing for TIM regions. This class encapsulates all geometry-related conversions and
 * calculations.
 */
@Slf4j
@Component
public class TimGeometryProcessor {

    // Constants
    private static final double DEFAULT_PADDING_DEGREES = 0.005;
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
            List<List<Double>> coordinates = processRegionCoordinates(region);

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
        if (dataFrame.getRegions() == null || dataFrame.getRegions().isEmpty()) {
            return null;
        }

        if (dataFrame.getRegions().size() == 1) {
            return createGeometryFromRegion(dataFrame.getRegions().get(0));
        } else {
            return createMultiGeometryFromRegions(dataFrame.getRegions());
        }
    }

    /**
     * Extract elevation and lane width offset information from a region's path.
     *
     * @param region The geographical path region
     * @return OffsetInformation containing elevation and lane width offsets, or null if no path
     */
    public OffsetInformation extractOffsetInformation(GeographicalPath region) {
        if (region.getDescription() == null || region.getDescription().getPath() == null) {
            return null;
        }

        List<PathNodeData> pathData = processOffsetPathWithOffsets(region, region.getDescription().getPath());
        if (pathData.isEmpty()) {
            return null;
        }

        List<Long> elevationOffsets = new ArrayList<>();
        List<Long> laneWidthOffsets = new ArrayList<>();

        for (PathNodeData nodeData : pathData) {
            if (nodeData.getDelevationOffset() != null) {
                elevationOffsets.add(nodeData.getDelevationOffset());
            }
            if (nodeData.getDwidthOffset() != null) {
                laneWidthOffsets.add(nodeData.getDwidthOffset());
            }
        }

        return new OffsetInformation(elevationOffsets.isEmpty() ? null : elevationOffsets,
                laneWidthOffsets.isEmpty() ? null : laneWidthOffsets);
    }

    /**
     * Calculate the center location from all regions in the TIM message.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @return JTS Point representing the center location, or null if no regions found
     */
    public Point calculateCenterLocationFromRegions(TravelerInformation travelerInfo) {
        List<List<Double>> coordinates = new ArrayList<>();

        if (travelerInfo.getDataFrames() != null) {
            for (TravelerDataFrame dataFrame : travelerInfo.getDataFrames()) {
                if (dataFrame.getRegions() != null) {
                    for (GeographicalPath region : dataFrame.getRegions()) {
                        // Get anchor point coordinates
                        if (region.getAnchor() != null) {
                            double lat = FieldConversions.convertLat(region.getAnchor().getLat().getValue());
                            double lon = FieldConversions.convertLong(region.getAnchor().getLong_().getValue());
                            coordinates.add(Arrays.asList(lon, lat)); // [longitude, latitude]
                        }
                    }
                }
            }
        }

        // Calculate center location using Geotools GeodeticCalculator
        if (coordinates.isEmpty()) {
            return null;
        }

        // Calculate center point (simple average)
        double centerLat = coordinates.stream().filter(coord -> coord != null && coord.size() >= 2)
                .mapToDouble(coord -> coord.get(1)) // latitude
                .average().orElse(0.0);

        double centerLon = coordinates.stream().filter(coord -> coord != null && coord.size() >= 2)
                .mapToDouble(coord -> coord.get(0)) // longitude
                .average().orElse(0.0);

        // Create Point geometry using JTS GeometryFactory
        return new GeometryFactory().createPoint(new Coordinate(centerLon, centerLat));
    }

    /**
     * Determine the region type from ASN.1 data.
     */
    private ProcessedRegionType determineRegionType(GeographicalPath region) {
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
     * Process region coordinates from ASN.1 data
     */
    private List<List<Double>> processRegionCoordinates(GeographicalPath region) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Get coordinates from the description
        DescriptionChoice description = region.getDescription();
        if (description != null) {
            if (description.getPath() != null) {
                coordinates.addAll(processOffsetPath(region, description.getPath()));
            } else if (description.getGeometry() != null) {
                coordinates.addAll(processGeometry(region, description.getGeometry()));
            }
        }

        // If no description, try to use anchor point with lane width
        if (coordinates.isEmpty() && region.getAnchor() != null) {
            Position3D anchor = region.getAnchor();
            coordinates.addAll(createRectangleFromAnchor(anchor));
        }

        return coordinates;
    }

    /**
     * Create geometry based on the region type.
     */
    private Geometry createGeometryByType(List<List<Double>> coordinates, ProcessedRegionType regionType) {
        switch (regionType) {
            case CIRCLE:
            case POLYGON:
                return createPolygonFromCoordinates(coordinates);
            case PATH:
            case UNKNOWN:
            default:
                return createLineStringFromCoordinates(coordinates);
        }
    }

    /**
     * Create MultiLineString or MultiPolygon from multiple regions
     */
    private Geometry createMultiGeometryFromRegions(List<GeographicalPath> regions) {
        List<List<List<Double>>> allCoordinates = new ArrayList<>();
        boolean hasPolygons = false;

        // Process each region and determine geometry types
        for (GeographicalPath region : regions) {
            List<List<Double>> coordinates = processRegionCoordinates(region);
            if (!coordinates.isEmpty()) {
                allCoordinates.add(coordinates);

                // Determine if this region should be a polygon or linestring
                boolean isClosedPath = region.getClosedPath() != null && region.getClosedPath().getValue();
                boolean isCircle = region.getDescription() != null && region.getDescription().getGeometry() != null
                        && region.getDescription().getGeometry().getCircle() != null;

                if (isCircle || isClosedPath) {
                    hasPolygons = true;
                }
            }
        }

        if (allCoordinates.isEmpty()) {
            return null;
        }

        // If we have polygons, use MultiPolygon, otherwise use MultiLineString
        if (hasPolygons) {
            return createMultiPolygonFromCoordinates(allCoordinates);
        } else {
            return createMultiLineStringFromCoordinates(allCoordinates);
        }
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
    private long[] extractNodeOffsets(NodeLL node) {
        if (node.getAttributes() != null) {
            long dwidthOffset = 0;
            long delevationOffset = 0;

            if (node.getAttributes().getDWidth() != null) {
                dwidthOffset = node.getAttributes().getDWidth().getValue();
            }
            if (node.getAttributes().getDElevation() != null) {
                delevationOffset = node.getAttributes().getDElevation().getValue();
            }

            return new long[] {dwidthOffset, delevationOffset};
        }
        return null;
    }

    /**
     * Extract dwidth and delevation offsets from node attributes.
     * 
     * @param node The node to extract offsets from
     * @return Array containing [dwidthOffset, delevationOffset] or null if no attributes
     */
    private long[] extractNodeOffsets(NodeXY node) {
        if (node.getAttributes() != null) {
            long dwidthOffset = 0;
            long delevationOffset = 0;

            if (node.getAttributes().getDWidth() != null) {
                dwidthOffset = node.getAttributes().getDWidth().getValue();
            }
            if (node.getAttributes().getDElevation() != null) {
                delevationOffset = node.getAttributes().getDElevation().getValue();
            }

            return new long[] {dwidthOffset, delevationOffset};
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
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL2() != null) {
            var nodeLL2 = node.getNode_LL2();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL2.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL2.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL3() != null) {
            var nodeLL3 = node.getNode_LL3();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL3.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL3.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL4() != null) {
            var nodeLL4 = node.getNode_LL4();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL4.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL4.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL5() != null) {
            var nodeLL5 = node.getNode_LL5();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL5.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL5.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LL6() != null) {
            var nodeLL6 = node.getNode_LL6();
            Double lonOffset = FieldConversions.convertLongWithZoom(nodeLL6.getLon().getValue(), zoomFactor);
            Double latOffset = FieldConversions.convertLatWithZoom(nodeLL6.getLat().getValue(), zoomFactor);
            if (lonOffset != null)
                currentLon += lonOffset;
            if (latOffset != null)
                currentLat += latOffset;
        } else if (node.getNode_LatLon() != null) {
            var nodeLatLon = node.getNode_LatLon();
            // node_LatLon contains absolute coordinates, not offsets - doesn't require anchor
            Double absLon = FieldConversions.convertLong(nodeLatLon.getLon().getValue());
            Double absLat = FieldConversions.convertLat(nodeLatLon.getLat().getValue());
            if (absLon != null)
                currentLon = absLon;
            if (absLat != null)
                currentLat = absLat;
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
     * Process XY (Cartesian) node and update current coordinates. Note: XY nodes are always offsets and require a valid
     * starting point (anchor).
     * 
     * @param node The node to process
     * @param zoomFactor Zoom scaling factor
     * @param currentCoords Current coordinates [lon, lat] to update. Must be initialized with anchor coordinates.
     */
    private void processXYNode(NodeOffsetPointXY node, double zoomFactor, double[] currentCoords) {
        // XY nodes require a starting point for offset calculations
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
        }

        // Update coordinates array
        currentCoords[0] = currentLon;
        currentCoords[1] = currentLat;
    }

    /**
     * Process offset path and return both coordinates and offset information.
     * 
     * @param region The geographical path region
     * @param path The offset system path
     * @return List of PathNodeData containing coordinates and offset information
     */
    private List<PathNodeData> processOffsetPathWithOffsets(GeographicalPath region, OffsetSystem path) {
        if (path == null || region.getAnchor() == null) {
            return new ArrayList<>();
        }

        Position3D anchor = region.getAnchor();
        double anchorLat = FieldConversions.convertLat(anchor.getLat().getValue());
        double anchorLon = FieldConversions.convertLong(anchor.getLong_().getValue());

        List<PathNodeData> pathData = new ArrayList<>();
        // Add anchor point with no offsets
        pathData.add(new PathNodeData(Arrays.asList(anchorLon, anchorLat), null, null));

        if (path.getOffset() != null) {
            double[] currentCoords = {anchorLon, anchorLat};
            double zoomFactor = calculateZoomFactor(path);

            // Handle LL (Latitude/Longitude) coordinates
            if (path.getOffset().getLl() != null && path.getOffset().getLl().getNodes() != null) {
                for (var node : path.getOffset().getLl().getNodes()) {
                    if (node.getDelta() != null) {
                        processLLNode(node.getDelta(), zoomFactor, currentCoords);
                        long[] offsets = extractNodeOffsets(node);
                        Long dwidthOffset = offsets != null ? offsets[0] : null;
                        Long delevationOffset = offsets != null ? offsets[1] : null;
                        pathData.add(new PathNodeData(Arrays.asList(currentCoords[0], currentCoords[1]), dwidthOffset,
                                delevationOffset));
                    }
                }
            }
            // Handle XY (Cartesian) coordinates
            else if (path.getOffset().getXy() != null && path.getOffset().getXy().getNodes() != null) {
                for (var node : path.getOffset().getXy().getNodes()) {
                    if (node.getDelta() != null) {
                        processXYNode(node.getDelta(), zoomFactor, currentCoords);
                        long[] offsets = extractNodeOffsets(node);
                        Long dwidthOffset = offsets != null ? offsets[0] : null;
                        Long delevationOffset = offsets != null ? offsets[1] : null;
                        pathData.add(new PathNodeData(Arrays.asList(currentCoords[0], currentCoords[1]), dwidthOffset,
                                delevationOffset));
                    }
                }
            }
        }

        return pathData;
    }

    private List<List<Double>> processOffsetPath(GeographicalPath region, OffsetSystem path) {
        if (path == null) {
            return new ArrayList<>();
        }

        List<List<Double>> coordinates = new ArrayList<>();

        // Initialize anchor coordinates if available
        double anchorLat = 0.0;
        double anchorLon = 0.0;
        boolean hasAnchor = false;

        if (region.getAnchor() != null) {
            Position3D anchor = region.getAnchor();
            anchorLat = FieldConversions.convertLat(anchor.getLat().getValue());
            anchorLon = FieldConversions.convertLong(anchor.getLong_().getValue());
            hasAnchor = true;
        }

        if (path.getOffset() != null) {
            double zoomFactor = calculateZoomFactor(path);

            // Handle LL (Latitude/Longitude) coordinates
            if (path.getOffset().getLl() != null && path.getOffset().getLl().getNodes() != null) {
                // Initialize currentCoords with anchor if available, otherwise use default
                double[] currentCoords = hasAnchor ? new double[] {anchorLon, anchorLat} : new double[] {0.0, 0.0};

                for (var node : path.getOffset().getLl().getNodes()) {
                    if (node.getDelta() != null) {
                        // Check if this is a LatLon node (absolute coordinates) that doesn't need anchor
                        boolean isLatLonNode = node.getDelta().getNode_LatLon() != null;

                        // Process if we have anchor OR if it's a LatLon node (absolute coordinates)
                        if (hasAnchor || isLatLonNode) {
                            processLLNode(node.getDelta(), zoomFactor, currentCoords);
                            coordinates.add(Arrays.asList(currentCoords[0], currentCoords[1]));
                        }
                    }
                }
            }
            // Handle XY (Cartesian) coordinates - require anchor for offset calculations
            else if (path.getOffset().getXy() != null && path.getOffset().getXy().getNodes() != null) {
                if (hasAnchor) {
                    double[] currentCoords = {anchorLon, anchorLat};
                    for (var node : path.getOffset().getXy().getNodes()) {
                        if (node.getDelta() != null) {
                            processXYNode(node.getDelta(), zoomFactor, currentCoords);
                            coordinates.add(Arrays.asList(currentCoords[0], currentCoords[1]));
                        }
                    }
                }
            }
        }

        return coordinates;
    }

    private List<List<Double>> processGeometry(GeographicalPath region, GeometricProjection geometry) {
        if (geometry == null || region.getAnchor() == null) {
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
                double centerLat = FieldConversions.convertLat(circle.getCenter().getLat().getValue());
                double centerLon = FieldConversions.convertLong(circle.getCenter().getLong_().getValue());

                if (radiusMeters != null) {
                    // Create circle points using accurate geodetic calculations
                    coordinates.addAll(createCirclePoints(centerLon, centerLat, radiusMeters.intValue()));
                } else {
                    log.error("Invalid circle radius: {}", radius);
                }
            } else {
                log.warn("Circle geometry missing radius or center field");
            }
        }

        return coordinates;
    }

    /**
     * Create circle points using UTM coordinates for accurate geodetic calculations. Circle is generated in UTM space
     * and then converted back to WGS84 using ProjectionUtils for coordinate transformations.
     * 
     * @param centerLon Center longitude in degrees
     * @param centerLat Center latitude in degrees
     * @param radiusMeters Radius in meters
     * @return List of coordinate points forming a circle
     */
    private List<List<Double>> createCirclePoints(double centerLon, double centerLat, int radiusMeters) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Validate center coordinates
        if (centerLon < -180.0 || centerLon > 180.0 || centerLat < -90.0 || centerLat > 90.0) {
            log.error("Invalid circle center coordinates: lon={}, lat={}", centerLon, centerLat);
            return coordinates;
        }

        try {
            // Get UTM CRS code for the location
            String utmCrsCode = ProjectionUtils.getUtmCrsCode(centerLon, centerLat);
            int utmZone = ProjectionUtils.getUtmZone(centerLon);

            // Create coordinate transforms using ProjectionUtils
            CoordinateTransform wgsToUtm = ProjectionUtils.createTransform("EPSG:4326", utmCrsCode);
            CoordinateTransform utmToWgs = ProjectionUtils.createTransform(utmCrsCode, "EPSG:4326");

            if (wgsToUtm == null || utmToWgs == null) {
                log.error("Failed to create coordinate transforms for UTM zone {}", utmZone);
                return coordinates; // Return empty coordinates if transforms fail
            }

            // Transform center point to UTM using ProjectionUtils
            ProjCoordinate centerUTM =
                    ProjectionUtils.transformCoordinate("EPSG:4326", utmCrsCode, centerLon, centerLat);

            // Calculate adaptive number of points based on diameter
            double diameterMeters = radiusMeters * 2.0;
            int adaptivePoints = calculateAdaptiveCirclePoints(diameterMeters);

            // Create circle in UTM using JTS GeometricShapeFactory
            GeometricShapeFactory shapeFactory = new GeometricShapeFactory(new GeometryFactory());
            shapeFactory.setCentre(new Coordinate(centerUTM.x, centerUTM.y));
            shapeFactory.setSize(diameterMeters);
            shapeFactory.setNumPoints(adaptivePoints);

            org.locationtech.jts.geom.Polygon circleUTM = shapeFactory.createCircle();
            Coordinate[] circleCoordsUTM = circleUTM.getExteriorRing().getCoordinates();

            // Transform circle coordinates from UTM back to WGS84 using ProjectionUtils
            for (Coordinate coordUTM : circleCoordsUTM) {
                ProjCoordinate coordWGS84 = new ProjCoordinate();
                utmToWgs.transform(new ProjCoordinate(coordUTM.x, coordUTM.y), coordWGS84);
                coordinates.add(Arrays.asList(coordWGS84.x, coordWGS84.y));
            }

            log.debug("Created UTM-based circle with {} points, center=({}, {}), radius={}m, diameter={}m, UTM zone={}",
                    adaptivePoints, centerLon, centerLat, radiusMeters, diameterMeters, utmZone);

        } catch (Exception e) {
            log.error("Error creating UTM-based circle: {}", e.getMessage(), e);
        }

        return coordinates;
    }

    private List<List<Double>> createRectangleFromAnchor(Position3D anchor) {
        if (anchor == null) {
            return new ArrayList<>();
        }

        double lat = FieldConversions.convertLat(anchor.getLat().getValue());
        double lon = FieldConversions.convertLong(anchor.getLong_().getValue());

        // Create rectangle using GeodeticUtils for accurate coordinate calculations
        return createRectanglePoints(lon, lat, DEFAULT_PADDING_DEGREES);
    }

    /**
     * Create rectangle points using JTS GeometricShapeFactory.
     * 
     * @param centerLon Center longitude in degrees
     * @param centerLat Center latitude in degrees
     * @param paddingDegrees Padding in degrees (half-width and half-height)
     * @return List of coordinate points forming a rectangle
     */
    private List<List<Double>> createRectanglePoints(double centerLon, double centerLat, double paddingDegrees) {
        List<List<Double>> coordinates = new ArrayList<>();

        // Create rectangle using JTS GeometricShapeFactory
        GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
        shapeFactory.setCentre(new Coordinate(centerLon, centerLat));
        shapeFactory.setWidth(paddingDegrees * 2); // total width
        shapeFactory.setHeight(paddingDegrees * 2); // total height

        org.locationtech.jts.geom.Polygon rectangle = shapeFactory.createRectangle();
        Coordinate[] rectCoords = rectangle.getExteriorRing().getCoordinates();

        for (Coordinate coord : rectCoords) {
            coordinates.add(Arrays.asList(coord.x, coord.y));
        }

        return coordinates;
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

    private MultiLineString createMultiLineStringFromCoordinates(List<List<List<Double>>> allCoordinates) {
        if (allCoordinates == null || allCoordinates.isEmpty()) {
            return null;
        }

        double[][][] coordinateArray = new double[allCoordinates.size()][][];
        for (int i = 0; i < allCoordinates.size(); i++) {
            List<List<Double>> lineString = allCoordinates.get(i);
            if (lineString != null && !lineString.isEmpty()) {
                coordinateArray[i] = new double[lineString.size()][2];
                for (int j = 0; j < lineString.size(); j++) {
                    List<Double> coord = lineString.get(j);
                    if (coord.size() >= 2) {
                        coordinateArray[i][j][0] = coord.get(0); // longitude
                        coordinateArray[i][j][1] = coord.get(1); // latitude
                    }
                }
            }
        }

        return new MultiLineString(coordinateArray);
    }

    private MultiPolygon createMultiPolygonFromCoordinates(List<List<List<Double>>> allCoordinates) {
        if (allCoordinates == null || allCoordinates.isEmpty()) {
            return null;
        }

        double[][][][] coordinateArray = new double[allCoordinates.size()][][][];
        for (int i = 0; i < allCoordinates.size(); i++) {
            List<List<Double>> polygon = allCoordinates.get(i);
            if (polygon != null && !polygon.isEmpty()) {
                // Ensure the polygon is closed
                List<List<Double>> closedPolygon = new ArrayList<>(polygon);
                if (closedPolygon.size() > 2) {
                    List<Double> first = closedPolygon.get(0);
                    List<Double> last = closedPolygon.get(closedPolygon.size() - 1);
                    if (!first.equals(last)) {
                        closedPolygon.add(new ArrayList<>(first));
                    }
                }

                coordinateArray[i] = new double[1][closedPolygon.size()][2];
                for (int j = 0; j < closedPolygon.size(); j++) {
                    List<Double> coord = closedPolygon.get(j);
                    if (coord.size() >= 2) {
                        coordinateArray[i][0][j][0] = coord.get(0); // longitude
                        coordinateArray[i][0][j][1] = coord.get(1); // latitude
                    }
                }
            }
        }

        return new MultiPolygon(coordinateArray);
    }
}
