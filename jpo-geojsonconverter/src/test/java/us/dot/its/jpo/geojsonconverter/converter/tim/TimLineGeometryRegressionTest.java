package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import us.dot.its.jpo.geojsonconverter.pojos.geojson.MultiLineString;

class TimLineGeometryRegressionTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void repeatedCrossingsKeepTraversalOrderAcrossSeamAlias(boolean reversed) {
        double[][] coordinates = reversed
                ? new double[][] {{170, 40}, {-170, 35}, {180, 30}, {-180, 25}, {-170, 20}, {170, 10}}
                : new double[][] {{170, 10}, {-170, 20}, {-180, 25}, {180, 30}, {-170, 35}, {170, 40}};
        double[][][] expected = reversed
                ? new double[][][] {
                        {{170, 40}, {180, 37.5}},
                        {{-180, 37.5}, {-170, 35}, {-180, 30}},
                        {{180, 30}, {180, 30}, {180, 25}},
                        {{-180, 25}, {-170, 20}, {-180, 15}},
                        {{180, 15}, {170, 10}}
                }
                : new double[][][] {
                        {{170, 10}, {180, 15}},
                        {{-180, 15}, {-170, 20}, {-180, 25}, {-180, 30}, {-170, 35}, {-180, 37.5}},
                        {{180, 37.5}, {170, 40}}
                };

        MultiLineString result = assertInstanceOf(MultiLineString.class, TimLineGeometry.toGeoJson(coordinates));

        assertEquals(expected.length, result.getCoordinates().length);
        for (int piece = 0; piece < expected.length; piece++) {
            assertEquals(expected[piece].length, result.getCoordinates()[piece].length);
            for (int point = 0; point < expected[piece].length; point++) {
                assertArrayEquals(expected[piece][point], result.getCoordinates()[piece][point]);
            }
        }
    }

    @org.junit.jupiter.api.Test
    void identicalSeamAliasesDoNotProduceAnEmptyLinePiece() {
        assertNull(TimLineGeometry.toGeoJson(new double[][] {{180, 40}, {-180, 40}, {180, 40}}));
    }
}
