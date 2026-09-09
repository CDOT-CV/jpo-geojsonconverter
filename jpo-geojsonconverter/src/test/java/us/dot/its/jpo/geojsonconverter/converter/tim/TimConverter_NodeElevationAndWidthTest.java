package us.dot.its.jpo.geojsonconverter.converter.tim;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import us.dot.its.jpo.asn.j2735.r2024.Common.NodeSetXY;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Arrays;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@Slf4j
public class TimConverter_NodeElevationAndWidthTest {

    private static final String TIM_WITH_NODE_ELEVATION_AND_WIDTH = "/json/sample.tim-node-elevation-and-width.json";
    private static final String TIM_WITH_NODE_ELEVATION_AND_WIDTH_TWO_REGIONS =
            "/json/sample.tim-node-elevation-and-width-two-regions.json";

    @Test
    public void testElevationProfileMatchesNumberOfOfssetNodes_OneRegion() throws IOException {
        testElevationAndWidthProfileNumberOfNodes(loadResource(TIM_WITH_NODE_ELEVATION_AND_WIDTH));
    }

    @Test
    public void testElevationAndWidthProfileNumberOfNodes_TwoRegions() throws IOException {
        testElevationAndWidthProfileNumberOfNodes(loadResource(TIM_WITH_NODE_ELEVATION_AND_WIDTH_TWO_REGIONS));
    }

    @Test
    public void testLaneWidthOffsetsWithoutDefaultProduceUnknownNodeWidths() throws IOException {
        var mapper = DateJsonMapper.getInstance();
        TravelerInformation tim = mapper.readValue(loadResource(TIM_WITH_NODE_ELEVATION_AND_WIDTH),
                TravelerInformation.class);
        GeographicalPath path = tim.getDataFrames().get(0).getRegions().get(0);
        path.setLaneWidth(null);

        TimConverter timConverter = new TimConverter(new TimGeometryConverter());
        ProcessedTim processedTim = timConverter.createProcessedTim(tim, new OdeMessageFrameMetadata());
        ProcessedPathRegionInfo pathRegionInfo = (ProcessedPathRegionInfo) processedTim.getDataFrameFeatureCollection()
                .getFeatures().get(0).getProperties().getRegionInfoList().get(0);

        ProcessedLaneWidthProfile widthProfile = pathRegionInfo.getLaneWidthProfile();
        assertThat(widthProfile, notNullValue());
        assertThat(widthProfile.getDefaultWidthMeters(), nullValue());
        assertThat(widthProfile.getNodeLaneWidthMeters(),
                equalTo(Arrays.<Double>asList(null, null, null, null, null, null, null, null, null)));
    }

    private void testElevationAndWidthProfileNumberOfNodes(String travelerInformationJson)
            throws JsonProcessingException {
        var mapper = DateJsonMapper.getInstance();
        TravelerInformation tim = mapper.readValue(travelerInformationJson, TravelerInformation.class);
        OdeMessageFrameMetadata mockMetadata = new OdeMessageFrameMetadata();
        TimConverter timConverter = new TimConverter(new TimGeometryConverter());

        ProcessedTim processedTim = timConverter.createProcessedTim(tim, mockMetadata);
        String json = mapper.writeValueAsString(processedTim);
        log.info(json);
        int numberOfFrames = tim.getDataFrames().size();

        List<ProcessedTimFeature<?>> features = processedTim.getDataFrameFeatureCollection().getFeatures();
        assertThat("number of features should match number of data frames", features, hasSize(equalTo(numberOfFrames)));

        for (int frameNum = 0; frameNum < numberOfFrames; frameNum++) {
            TravelerDataFrame frame = tim.getDataFrames().get(frameNum);
            TravelerDataFrame.SequenceOfRegions paths = frame.getRegions();
            int numberOfPaths = paths.size();

            ProcessedTimFeature<?> feature = features.get(frameNum);

            List<ProcessedRegionInfoBase> regionInfoList = feature.getProperties().getRegionInfoList();
            assertThat("number of regions should match number of paths", regionInfoList,
                    hasSize(equalTo(numberOfPaths)));

            for (int i = 0; i < numberOfPaths; i++) {

                ProcessedRegionInfoBase regionInfo = regionInfoList.get(i);
                assertThat(regionInfo, instanceOf(ProcessedPathRegionInfo.class));
                ProcessedPathRegionInfo pathRegionInfo = (ProcessedPathRegionInfo) regionInfo;

                GeographicalPath path = paths.get(i);
                NodeSetXY pathNodes = path.getDescription().getPath().getOffset().getXy().getNodes();
                int numPathNodes = pathNodes.size();

                // Check elevation profile
                ProcessedElevationProfile elevationProfile = pathRegionInfo.getElevationProfile();
                List<Double> elevationPoints = elevationProfile.getNodeElevationMeters();
                assertThat("number or elevation points should match number of nodes", elevationPoints,
                        hasSize(equalTo(numPathNodes)));

                // Check width profile
                ProcessedLaneWidthProfile widthProfile = pathRegionInfo.getLaneWidthProfile();
                List<Double> widthPoints = widthProfile.getNodeLaneWidthMeters();
                assertThat("number or width points should match number of nodes", widthPoints,
                        hasSize(equalTo(numPathNodes)));

                if (numPathNodes == 9) {
                    assertThat(elevationPoints,
                            equalTo(Arrays.asList(100.0, 102.0, 102.0, 102.0, 102.0, 99.0, 99.0, 99.0, 99.0)));
                    assertThat(widthPoints,
                            equalTo(Arrays.asList(15.0, 15.5, 15.5, 15.5, 15.5, 16.0, 16.0, 16.0, 16.0)));
                } else if (numPathNodes == 4) {
                    assertThat(elevationPoints, equalTo(Arrays.<Double>asList(null, null, null, null)));
                    assertThat(widthPoints, equalTo(Arrays.asList(15.0, 16.0, 17.0, 16.0)));
                }
            }
        }
    }

    private String loadResource(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Missing test resource: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
