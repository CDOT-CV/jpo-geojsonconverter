package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import us.dot.its.jpo.asn.j2735.r2024.Common.*;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.asn.runtime.types.Asn1Boolean;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.GeometryCollection;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiPolygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Polygon;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.ProcessedPathRegionInfo;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.serialization.JsonSerdes;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;

class TimAntimeridianTest {
    private final TimGeometryConverter converter = new TimGeometryConverter();
    private final GeometryFactory factory = new GeometryFactory();

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void closedLlRectangleContainsBothDatelineSidesAndExcludesGreenwich(boolean reversed) {
        GeographicalPath region = datelineRectangle(reversed);
        MultiPolygon result = assertInstanceOf(MultiPolygon.class, converter.createGeometryFromRegion(region));
        var actual = toJts(result);

        assertEquals(2, result.getCoordinates().length);
        assertTrue(actual.isValid());
        assertEquals(0.000002, actual.getArea(), 1e-12);
        assertTrue(actual.contains(factory.createPoint(new Coordinate(179.9995, 40.0005))));
        assertTrue(actual.contains(factory.createPoint(new Coordinate(-179.9995, 40.0005))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(0, 40.0005))));
        assertBoundedRings(result);
    }

    @Test
    void relativeLlPolygonIsSplitAfterAccumulatingOffsets() {
        GeographicalPath region = anchoredClosedPath(1799999900L, 400000000L);
        NodeSetLL nodes = new NodeSetLL();
        for (long[] value : new long[][] {{-1000, 0}, {2000, 0}, {0, 1000}, {-2000, 0}}) {
            var deltaValue = new Node_LL_24B();
            deltaValue.setLon(new OffsetLL_B12(value[0]));
            deltaValue.setLat(new OffsetLL_B12(value[1]));
            var delta = new NodeOffsetPointLL();
            delta.setNode_LL1(deltaValue);
            var node = new NodeLL();
            node.setDelta(delta);
            nodes.add(node);
        }
        region.getDescription().getPath().setOffset(llOffset(nodes));

        MultiPolygon result = assertInstanceOf(MultiPolygon.class, converter.createGeometryFromRegion(region));
        assertTrue(toJts(result).contains(factory.createPoint(new Coordinate(179.99995, 40.00005))));
        assertTrue(toJts(result).contains(factory.createPoint(new Coordinate(-179.99995, 40.00005))));
        assertFalse(toJts(result).contains(factory.createPoint(new Coordinate(0, 40.00005))));
        assertBoundedRings(result);
    }

    @Test
    void relativeXyPolygonIsSplitAfterAccumulatingOffsets() {
        GeographicalPath region = anchoredClosedPath(1799999990L, 400000000L);
        NodeSetXY nodes = new NodeSetXY();
        for (long[] value : new long[][] {{-100, 0}, {200, 0}, {0, 100}, {-200, 0}}) {
            var deltaValue = new Node_XY_20b();
            deltaValue.setX(new Offset_B10(value[0]));
            deltaValue.setY(new Offset_B10(value[1]));
            var delta = new NodeOffsetPointXY();
            delta.setNode_XY1(deltaValue);
            var node = new NodeXY();
            node.setDelta(delta);
            nodes.add(node);
        }
        var list = new NodeListXY();
        list.setNodes(nodes);
        var offset = new OffsetSystem.OffsetChoice();
        offset.setXy(list);
        region.getDescription().getPath().setOffset(offset);

        MultiPolygon result = assertInstanceOf(MultiPolygon.class, converter.createGeometryFromRegion(region));
        assertTrue(toJts(result).isValid());
        assertTrue(toJts(result).contains(factory.createPoint(new Coordinate(179.999995, 40.000005))));
        assertTrue(toJts(result).contains(factory.createPoint(new Coordinate(-179.999995, 40.000005))));
        assertFalse(toJts(result).contains(factory.createPoint(new Coordinate(0, 40.000005))));
        assertBoundedRings(result);
    }

    @ParameterizedTest
    @ValueSource(longs = {1799999000L, -1799999000L})
    void crossingCircleContainsItsCenterAndBothSidesOfTheDateline(long longitude) {
        MultiPolygon result = assertInstanceOf(MultiPolygon.class,
                converter.createGeometryFromRegion(circle(longitude, 400000000L, 100)));
        var actual = toJts(result);
        assertTrue(actual.isValid());
        assertTrue(actual.contains(factory.createPoint(new Coordinate((double) longitude / 10000000.0, 40))));
        assertTrue(actual.contains(factory.createPoint(new Coordinate(179.9999, 40))));
        assertTrue(actual.contains(factory.createPoint(new Coordinate(-179.9999, 40))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(0, 40))));
        assertBoundedRings(result);
    }

    @ParameterizedTest
    @ValueSource(longs = {899999000L, -899999000L})
    void circleEnclosingAPoleRetainsThePolarCap(long latitude) {
        Geometry result = converter.createGeometryFromRegion(circle(300000000L, latitude, 1000));
        var actual = toJts(result);
        assertTrue(actual.isValid());
        assertTrue(actual.contains(factory.createPoint(new Coordinate(0, Math.copySign(89.99999, (double) latitude)))));
        assertFalse(actual.contains(factory.createPoint(new Coordinate(0, 0))));
    }

    @Test
    void splitRegionKeepsOneGeometryIndexWhenOrdinaryAndInvalidRegionsCoexist() {
        GeographicalPath invalid = closedLlPath(new long[][] {{0, 0}, {10000000, 10000000}, {20000000, 20000000}});
        GeographicalPath ordinary = closedLlPath(new long[][] {
                {-1040000000, 400000000}, {-1039990000, 400000000},
                {-1039990000, 400010000}, {-1040000000, 400010000}});
        ProcessedTim result = processed(invalid, datelineRectangle(false), ordinary);
        var feature = result.getDataFrameFeatureCollection().getFeatures().getFirst();
        GeometryCollection geometry = assertInstanceOf(GeometryCollection.class, feature.getGeometry());

        assertEquals(2, geometry.getGeometries().length);
        assertInstanceOf(MultiPolygon.class, geometry.getGeometries()[0]);
        assertInstanceOf(Polygon.class, geometry.getGeometries()[1]);
        var regions = feature.getProperties().getRegionInfoList();
        assertNull(regions.get(0).getGeometryIndex());
        assertEquals(0, regions.get(1).getGeometryIndex());
        assertEquals(1, regions.get(2).getGeometryIndex());
        assertEquals(2, ((MultiPolygon) geometry.getGeometries()[regions.get(1).getGeometryIndex()])
                .getCoordinates().length);
    }

    @Test
    void onlySplitRegionKeepsAllPiecesUnderItsIndexAndRoundTripsThroughTheSerde() {
        ProcessedTim original = processed(datelineRectangle(false));
        try (var serde = JsonSerdes.ProcessedTim()) {
            byte[] json = serde.serializer().serialize("test", original);
            ProcessedTim result = serde.deserializer().deserialize("test", json);
            var feature = result.getDataFrameFeatureCollection().getFeatures().getFirst();
            GeometryCollection geometry = assertInstanceOf(GeometryCollection.class, feature.getGeometry());
            assertEquals(0, feature.getProperties().getRegionInfoList().getFirst().getGeometryIndex());
            assertEquals(1, geometry.getGeometries().length);
            MultiPolygon regionGeometry = assertInstanceOf(MultiPolygon.class, geometry.getGeometries()[0]);
            assertEquals(2, regionGeometry.getCoordinates().length);
            assertTrue(toJts(regionGeometry).contains(factory.createPoint(new Coordinate(-179.9995, 40.0005))));
        }
    }

    @Test
    void polygonTouchingTheDatelineDoesNotAcquireAnEmptyComponent() {
        Geometry result = converter.createGeometryFromRegion(closedLlPath(new long[][] {
                {1799990000, 400000000}, {1800000000, 400000000},
                {1800000000, 400010000}, {1799990000, 400010000}}));
        Polygon polygon = assertInstanceOf(Polygon.class, result);
        assertTrue(toJts(polygon).isValid());
        assertEquals(0.000001, toJts(polygon).getArea(), 1e-12);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void openPathsCrossTheDatelineWithoutIntersectingGreenwich(boolean reversed) {
        long[][] positions = reversed
                ? new long[][] {{-1799990000L, 400010000L}, {1799990000L, 400000000L}}
                : new long[][] {{1799990000L, 400000000L}, {-1799990000L, 400010000L}};
        GeographicalPath region = closedLlPath(positions);
        region.setClosedPath(new Asn1Boolean(false));
        MultiLineString result = assertInstanceOf(MultiLineString.class, converter.createGeometryFromRegion(region));
        assertEquals(2, result.getCoordinates().length);
        assertFalse(toJts(result).intersects(factory.createPoint(new Coordinate(0, 40.0005))));
        assertTrue(toJts(result).distance(factory.createPoint(new Coordinate(179.9995, 40.00025))) < 1e-8);
        assertTrue(toJts(result).distance(factory.createPoint(new Coordinate(-179.9995, 40.00075))) < 1e-8);
        double[][] first = result.getCoordinates()[0];
        double[][] last = result.getCoordinates()[1];
        assertEquals((double) positions[0][0] / 10000000.0, first[0][0]);
        assertEquals((double) positions[1][0] / 10000000.0, last[last.length - 1][0]);
        for (double[][] piece : result.getCoordinates()) {
            for (int i = 1; i < piece.length; i++) {
                assertTrue(Math.abs(piece[i][0] - piece[i - 1][0]) <= 180);
            }
        }
    }

    @Test
    void seamEndpointAndSeamAliasesDoNotCreateEmptyLinePiecesOrNonFiniteCoordinates() {
        GeographicalPath region = closedLlPath(new long[][] {{1800000000L, 400000000L}, {-1799990000L, 400010000L}});
        region.setClosedPath(new Asn1Boolean(false));
        LineString result = assertInstanceOf(LineString.class, converter.createGeometryFromRegion(region));
        assertEquals(2, result.getCoordinates().length);
        assertEquals(-180.0, result.getCoordinates()[0][0]);
        assertFalse(toJts(result).intersects(factory.createPoint(new Coordinate(0, 40.0005))));

        Geometry aliases = TimLineGeometry.toGeoJson(new double[][] {{180, 40}, {-180, 40.001}, {-179.999, 40.002}});
        assertTrue(toJts(aliases).isValid());
        for (Coordinate point : toJts(aliases).getCoordinates()) {
            assertTrue(Double.isFinite(point.x));
            assertTrue(Double.isFinite(point.y));
        }
    }

    @Test
    void splitPathRetainsItsRegionIndexAndRoundTrips() {
        GeographicalPath region = closedLlPath(new long[][] {{1799990000L, 400000000L}, {-1799990000L, 400000000L}});
        region.setClosedPath(new Asn1Boolean(false));
        ProcessedTim original = processed(region);
        try (var serde = JsonSerdes.ProcessedTim()) {
            ProcessedTim result = serde.deserializer().deserialize("test", serde.serializer().serialize("test", original));
            var feature = result.getDataFrameFeatureCollection().getFeatures().getFirst();
            GeometryCollection collection = assertInstanceOf(GeometryCollection.class, feature.getGeometry());
            assertEquals(0, feature.getProperties().getRegionInfoList().getFirst().getGeometryIndex());
            assertEquals(1, collection.getGeometries().length);
            assertEquals(2, assertInstanceOf(MultiLineString.class, collection.getGeometries()[0]).getCoordinates().length);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unavailableAbsoluteAndSkippedRelativeNodesKeepTheirPersistentAttributes(boolean xy) {
        GeographicalPath region = anchoredClosedPath(-1040000000L, 400000000L);
        region.setClosedPath(new Asn1Boolean(false));
        region.getAnchor().setElevation(new Elevation(1000));
        region.setLaneWidth(new LaneWidth(300));
        long[] increments = {10, 100, 20, 10};
        long[] longitudes = {-1040000000L, 1800000001L, -1040000000L, -1030000000L};
        NodeSetLL llNodes = new NodeSetLL();
        NodeSetXY xyNodes = new NodeSetXY();
        for (int i = 0; i < increments.length; i++) {
            var absolute = new Node_LLmD_64b();
            absolute.setLon(new Longitude(longitudes[i]));
            absolute.setLat(new Latitude(400000000L));
            if (xy) {
                var delta = new NodeOffsetPointXY();
                if (i == 2) {
                    var relative = new Node_XY_20b();
                    relative.setX(new Offset_B10(10)); relative.setY(new Offset_B10(0));
                    delta.setNode_XY1(relative);
                } else delta.setNode_LatLon(absolute);
                var node = new NodeXY(); node.setDelta(delta);
                var attributes = new NodeAttributeSetXY();
                attributes.setDWidth(new Offset_B10(increments[i]));
                attributes.setDElevation(new Offset_B10(increments[i]));
                node.setAttributes(attributes); xyNodes.add(node);
            } else {
                var delta = new NodeOffsetPointLL();
                if (i == 2) {
                    var relative = new Node_LL_24B();
                    relative.setLon(new OffsetLL_B12(10)); relative.setLat(new OffsetLL_B12(0));
                    delta.setNode_LL1(relative);
                } else delta.setNode_LatLon(absolute);
                var node = new NodeLL(); node.setDelta(delta);
                var attributes = new NodeAttributeSetLL();
                attributes.setDWidth(new Offset_B10(increments[i]));
                attributes.setDElevation(new Offset_B10(increments[i]));
                node.setAttributes(attributes); llNodes.add(node);
            }
        }
        if (xy) {
            var list = new NodeListXY(); list.setNodes(xyNodes);
            var offset = new OffsetSystem.OffsetChoice(); offset.setXy(list);
            region.getDescription().getPath().setOffset(offset);
        } else region.getDescription().getPath().setOffset(llOffset(llNodes));

        var feature = processed(region).getDataFrameFeatureCollection().getFeatures().getFirst();
        assertEquals(2, assertInstanceOf(LineString.class, feature.getGeometry()).getCoordinates().length);
        ProcessedPathRegionInfo properties = assertInstanceOf(ProcessedPathRegionInfo.class,
                feature.getProperties().getRegionInfoList().getFirst());
        assertArrayEquals(new double[] {100.1, 101.1, 101.3, 101.4}, properties.getElevationProfile()
                .getNodeElevationMeters().stream().mapToDouble(Double::doubleValue).toArray(), 1e-9);
        assertArrayEquals(new double[] {3.1, 4.1, 4.3, 4.4}, properties.getLaneWidthProfile()
                .getNodeLaneWidthMeters().stream().mapToDouble(Double::doubleValue).toArray(), 1e-9);

        region.getAnchor().setElevation(null);
        region.setLaneWidth(null);
        var unknown = assertInstanceOf(ProcessedPathRegionInfo.class, processed(region).getDataFrameFeatureCollection()
                .getFeatures().getFirst().getProperties().getRegionInfoList().getFirst());
        assertEquals(Arrays.asList(null, null, null, null), unknown.getElevationProfile().getNodeElevationMeters());
        assertEquals(Arrays.asList(null, null, null, null), unknown.getLaneWidthProfile().getNodeLaneWidthMeters());
    }

    private ProcessedTim processed(GeographicalPath... regions) {
        var frame = new TravelerDataFrame();
        frame.setRegions(new TravelerDataFrame.SequenceOfRegions());
        for (var region : regions) frame.getRegions().add(region);
        var information = new TravelerInformation();
        information.setDataFrames(new TravelerDataFrameList());
        information.getDataFrames().add(frame);
        var metadata = new OdeMessageFrameMetadata();
        metadata.setOdeReceivedAt("2026-10-06T12:00:00Z");
        return new TimConverter(converter).createProcessedTim(information, metadata);
    }

    private GeographicalPath datelineRectangle(boolean reversed) {
        List<long[]> positions = Arrays.asList(new long[] {1799990000L, 400000000L},
                new long[] {-1799990000L, 400000000L}, new long[] {-1799990000L, 400010000L},
                new long[] {1799990000L, 400010000L});
        if (reversed) Collections.reverse(positions);
        return closedLlPath(positions.toArray(long[][]::new));
    }

    private GeographicalPath closedLlPath(long[][] positions) {
        GeographicalPath region = anchoredClosedPath(0, 0);
        region.setAnchor(null);
        NodeSetLL nodes = new NodeSetLL();
        for (long[] point : positions) {
            var absolute = new Node_LLmD_64b();
            absolute.setLon(new Longitude(point[0]));
            absolute.setLat(new Latitude(point[1]));
            var delta = new NodeOffsetPointLL();
            delta.setNode_LatLon(absolute);
            var node = new NodeLL();
            node.setDelta(delta);
            nodes.add(node);
        }
        region.getDescription().getPath().setOffset(llOffset(nodes));
        return region;
    }

    private GeographicalPath anchoredClosedPath(long longitude, long latitude) {
        var region = new GeographicalPath();
        var anchor = new Position3D();
        anchor.setLong_(new Longitude(longitude));
        anchor.setLat(new Latitude(latitude));
        region.setAnchor(anchor);
        region.setClosedPath(new Asn1Boolean(true));
        var description = new GeographicalPath.DescriptionChoice();
        description.setPath(new OffsetSystem());
        region.setDescription(description);
        return region;
    }

    private OffsetSystem.OffsetChoice llOffset(NodeSetLL nodes) {
        var list = new NodeListLL();
        list.setNodes(nodes);
        var offset = new OffsetSystem.OffsetChoice();
        offset.setLl(list);
        return offset;
    }

    private GeographicalPath circle(long longitude, long latitude, long radius) {
        var center = new Position3D();
        center.setLong_(new Longitude(longitude));
        center.setLat(new Latitude(latitude));
        var circle = new Circle();
        circle.setCenter(center);
        circle.setRadius(new Radius_B12(radius));
        circle.setUnits(DistanceUnits.METER);
        var projection = new GeometricProjection();
        projection.setCircle(circle);
        var description = new GeographicalPath.DescriptionChoice();
        description.setGeometry(projection);
        var region = new GeographicalPath();
        region.setDescription(description);
        return region;
    }

    private org.locationtech.jts.geom.Geometry toJts(Geometry geometry) {
        if (geometry instanceof Polygon polygon) return polygon(polygon.getCoordinates());
        if (geometry instanceof LineString line) return line(line.getCoordinates());
        if (geometry instanceof MultiLineString lines) return factory.createMultiLineString(
                Arrays.stream(lines.getCoordinates()).map(this::line).toArray(org.locationtech.jts.geom.LineString[]::new));
        MultiPolygon multi = assertInstanceOf(MultiPolygon.class, geometry);
        return factory.createMultiPolygon(Arrays.stream(multi.getCoordinates()).map(this::polygon)
                .toArray(org.locationtech.jts.geom.Polygon[]::new));
    }

    private org.locationtech.jts.geom.LineString line(double[][] coordinates) {
        return factory.createLineString(Arrays.stream(coordinates).map(p -> new Coordinate(p[0], p[1]))
                .toArray(Coordinate[]::new));
    }

    private org.locationtech.jts.geom.Polygon polygon(double[][][] coordinates) {
        return factory.createPolygon(Arrays.stream(coordinates[0]).map(p -> new Coordinate(p[0], p[1]))
                .toArray(Coordinate[]::new));
    }

    private void assertBoundedRings(MultiPolygon geometry) {
        for (double[][][] polygon : geometry.getCoordinates()) {
            double[][] ring = polygon[0];
            assertArrayEquals(ring[0], ring[ring.length - 1]);
            for (int i = 1; i < ring.length; i++) {
                assertTrue(ring[i][0] >= -180 && ring[i][0] <= 180);
                assertTrue(ring[i][1] >= -90 && ring[i][1] <= 90);
                assertTrue(Math.abs(ring[i][0] - ring[i - 1][0]) <= 180);
            }
        }
    }
}
