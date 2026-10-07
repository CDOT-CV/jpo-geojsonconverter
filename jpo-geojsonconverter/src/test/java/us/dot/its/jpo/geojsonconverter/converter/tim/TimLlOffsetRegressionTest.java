package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import us.dot.its.jpo.asn.j2735.r2024.Common.Latitude;
import us.dot.its.jpo.asn.j2735.r2024.Common.Longitude;
import us.dot.its.jpo.asn.j2735.r2024.Common.Node_LLmD_64b;
import us.dot.its.jpo.asn.j2735.r2024.Common.OffsetLL_B18;
import us.dot.its.jpo.asn.j2735.r2024.Common.Position3D;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.*;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.LineString;

class TimLlOffsetRegressionTest {
    private static final long ANCHOR_LONGITUDE = -1041234567L;
    private static final long ANCHOR_LATITUDE = 399876543L;

    private final TimGeometryConverter geometryConverter = new TimGeometryConverter();

    @ParameterizedTest(name = "Relative LL offsets accumulate signed deltas at both scales")
    @MethodSource("llVariantCases")
    void relativeLlVariantsApplyZoomAndAccumulateBothAxes(LlVariantCase testCase) {
        GeographicalPath region = regionWithAnchor();

        for (int scaleIndex = 0; scaleIndex < 2; scaleIndex++) {
            long zoom = scaleIndex == 0 ? 2 : -1;
            double[][] expected = scaleIndex == 0 ? testCase.positiveScaleCoordinates()
                    : testCase.negativeScaleCoordinates();
            region.getDescription().getPath().setScale(new Zoom(zoom));
            region.getDescription().getPath().setOffset(createOffset(
                    node(relativeLlNode(testCase.variant(), testCase.firstLongitude(), testCase.firstLatitude())),
                    node(relativeLlNode(testCase.variant(), testCase.secondLongitude(), testCase.secondLatitude()))));

            Geometry geometry = geometryConverter.createGeometryFromRegion(region);
            LineString line = assertInstanceOf(LineString.class, geometry, "LL" + testCase.variant());
            assertEquals(expected.length, line.getCoordinates().length, "LL" + testCase.variant());
            for (int coordinateIndex = 0; coordinateIndex < expected.length; coordinateIndex++) {
                assertEquals(expected[coordinateIndex][0], line.getCoordinates()[coordinateIndex][0], 0.00000001,
                        "LL" + testCase.variant() + " longitude at node " + coordinateIndex);
                assertEquals(expected[coordinateIndex][1], line.getCoordinates()[coordinateIndex][1], 0.00000001,
                        "LL" + testCase.variant() + " latitude at node " + coordinateIndex);
            }
        }
    }

    @Test
    void unavailableAbsoluteReferenceSkipsRelativeNodeAndLaterRecovers() {
        GeographicalPath region = regionWithAnchor();
        region.setAnchor(null);
        region.getDescription().getPath().setScale(new Zoom(-1));
        region.getDescription().getPath().setOffset(createOffset(
                node(absoluteNode(1800000001L, 410000000L)),
                node(relativeLlNode(6, -500000L, 250000L)),
                node(absoluteNode(-1040000000L, 410000000L)),
                node(relativeLlNode(6, -2500000L, 750000L))));

        LineString line = assertInstanceOf(LineString.class, geometryConverter.createGeometryFromRegion(region));
        assertEquals(2, line.getCoordinates().length);
        assertEquals(-104.0, line.getCoordinates()[0][0], 0.00000001);
        assertEquals(41.0, line.getCoordinates()[0][1], 0.00000001);
        assertEquals(-104.125, line.getCoordinates()[1][0], 0.00000001);
        assertEquals(41.0375, line.getCoordinates()[1][1], 0.00000001);
    }

    private static Stream<Arguments> llVariantCases() {
        return Stream.of(
                Arguments.of(new LlVariantCase(1, 1200L, -300L, -400L, 900L,
                        new double[][] {{-104.1229767, 39.9875343}, {-104.1231367, 39.9878943}},
                        new double[][] {{-104.1233967, 39.9876393}, {-104.1234167, 39.9876843}})),
                Arguments.of(new LlVariantCase(2, 4000L, -2000L, -1000L, 3000L,
                        new double[][] {{-104.1218567, 39.9868543}, {-104.1222567, 39.9880543}},
                        new double[][] {{-104.1232567, 39.9875543}, {-104.1233067, 39.9877043}})),
                Arguments.of(new LlVariantCase(3, 30000L, -25000L, -10000L, 10000L,
                        new double[][] {{-104.1114567, 39.9776543}, {-104.1154567, 39.9816543}},
                        new double[][] {{-104.1219567, 39.9864043}, {-104.1224567, 39.9869043}})),
                Arguments.of(new LlVariantCase(4, 70000L, -60000L, -15000L, 25000L,
                        new double[][] {{-104.0954567, 39.9636543}, {-104.1014567, 39.9736543}},
                        new double[][] {{-104.1199567, 39.9846543}, {-104.1207067, 39.9859043}})),
                Arguments.of(new LlVariantCase(5, 300000L, -150000L, -100000L, 50000L,
                        new double[][] {{-104.0034567, 39.9276543}, {-104.0434567, 39.9476543}},
                        new double[][] {{-104.1084567, 39.9801543}, {-104.1134567, 39.9826543}})),
                Arguments.of(new LlVariantCase(6, 2000000L, -1200000L, -800000L, 400000L,
                        new double[][] {{-103.3234567, 39.5076543}, {-103.6434567, 39.6676543}},
                        new double[][] {{-104.0234567, 39.9276543}, {-104.0634567, 39.9476543}})));
    }

    private GeographicalPath regionWithAnchor() {
        GeographicalPath region = new GeographicalPath();
        Position3D anchor = new Position3D();
        anchor.setLong_(new Longitude(ANCHOR_LONGITUDE));
        anchor.setLat(new Latitude(ANCHOR_LATITUDE));
        region.setAnchor(anchor);

        GeographicalPath.DescriptionChoice description = new GeographicalPath.DescriptionChoice();
        description.setPath(new OffsetSystem());
        region.setDescription(description);
        return region;
    }

    private OffsetSystem.OffsetChoice createOffset(NodeLL... nodes) {
        NodeSetLL nodeSet = new NodeSetLL();
        for (NodeLL node : nodes) {
            nodeSet.add(node);
        }
        NodeListLL nodeList = new NodeListLL();
        nodeList.setNodes(nodeSet);
        OffsetSystem.OffsetChoice choice = new OffsetSystem.OffsetChoice();
        choice.setLl(nodeList);
        return choice;
    }

    private NodeLL node(NodeOffsetPointLL delta) {
        NodeLL node = new NodeLL();
        node.setDelta(delta);
        return node;
    }

    private NodeOffsetPointLL absoluteNode(long longitude, long latitude) {
        Node_LLmD_64b absolute = new Node_LLmD_64b();
        absolute.setLon(new Longitude(longitude));
        absolute.setLat(new Latitude(latitude));
        NodeOffsetPointLL delta = new NodeOffsetPointLL();
        delta.setNode_LatLon(absolute);
        return delta;
    }

    private NodeOffsetPointLL relativeLlNode(int variant, long longitude, long latitude) {
        NodeOffsetPointLL delta = new NodeOffsetPointLL();
        switch (variant) {
            case 1 -> {
                Node_LL_24B offsets = new Node_LL_24B();
                offsets.setLon(new OffsetLL_B12(longitude));
                offsets.setLat(new OffsetLL_B12(latitude));
                delta.setNode_LL1(offsets);
            }
            case 2 -> {
                Node_LL_28B offsets = new Node_LL_28B();
                offsets.setLon(new OffsetLL_B14(longitude));
                offsets.setLat(new OffsetLL_B14(latitude));
                delta.setNode_LL2(offsets);
            }
            case 3 -> {
                Node_LL_32B offsets = new Node_LL_32B();
                offsets.setLon(new OffsetLL_B16(longitude));
                offsets.setLat(new OffsetLL_B16(latitude));
                delta.setNode_LL3(offsets);
            }
            case 4 -> {
                Node_LL_36B offsets = new Node_LL_36B();
                offsets.setLon(new OffsetLL_B18(longitude));
                offsets.setLat(new OffsetLL_B18(latitude));
                delta.setNode_LL4(offsets);
            }
            case 5 -> {
                Node_LL_44B offsets = new Node_LL_44B();
                offsets.setLon(new OffsetLL_B22(longitude));
                offsets.setLat(new OffsetLL_B22(latitude));
                delta.setNode_LL5(offsets);
            }
            case 6 -> {
                Node_LL_48B offsets = new Node_LL_48B();
                offsets.setLon(new OffsetLL_B24(longitude));
                offsets.setLat(new OffsetLL_B24(latitude));
                delta.setNode_LL6(offsets);
            }
            default -> throw new IllegalArgumentException("Unexpected LL variant: " + variant);
        }
        return delta;
    }

    private record LlVariantCase(int variant, long firstLongitude, long firstLatitude, long secondLongitude,
            long secondLatitude, double[][] positiveScaleCoordinates, double[][] negativeScaleCoordinates) {
    }
}
