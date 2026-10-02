package us.dot.its.jpo.geojsonconverter.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static us.dot.its.jpo.geojsonconverter.TestResourceUtil.loadResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import us.dot.its.jpo.asn.j2735.r2024.MapData.MapData;
import us.dot.its.jpo.asn.j2735.r2024.MapData.MapDataMessageFrame;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.SPAT;
import us.dot.its.jpo.asn.j2735.r2024.SPAT.SPATMessageFrame;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.serialization.deserializers.JsonDeserializer;
import us.dot.its.jpo.ode.model.OdeMessageFrameData;

class CTI4501ValidatorTest {
    private static final String MAP_FRAME_PATH = "/payload/data/value/MapData";
    private static final String SPAT_FRAME_PATH = "/payload/data/value/SPAT";
    private static final String[] NODE_XY_CHOICES = {
            "node-XY1", "node-XY2", "node-XY3", "node-XY4", "node-XY5", "node-XY6"
    };

    @Test
    void reportsMissingSpatFieldsAndDeduplicatesRepeatedTimingIssues() throws IOException {
        SPAT spat = spatFrame();

        List<String> messages = messages(CTI4501Validator.spatValidation(spat));

        assertContains(messages, "SPAT 'timeStamp' DE_MinuteOfTheYear is missing");
        assertContains(messages, "intersections 'id.region' DE_RoadRegulatorID is missing");
        assertContains(messages, "state-time-speed 'timing.startTime' DE_TimeMark is missing");
        assertContains(messages, "state-time-speed 'timing.nextTime' DE_TimeMark is missing");
        assertFalse(contains(messages, "timing.maxEndTime"), "The fixture supplies maxEndTime");
        assertEquals(1, countContaining(messages, "timing.startTime"));
        assertEquals(1, countContaining(messages, "timing.nextTime"));
    }

    @Test
    void reportsMissingTimingContainer() throws IOException {
        ObjectNode root = spatFixture();
        ObjectNode firstEvent = (ObjectNode) root.at(SPAT_FRAME_PATH + "/intersections/0/states/0/state-time-speed/0");
        firstEvent.remove("timing");

        List<String> messages = messages(CTI4501Validator.spatValidation(spatFrame(root)));

        assertContains(messages, "state-time-speed 'timing' DF_TimeChangeDetails is missing");
    }

    @Test
    void reportsMissingIntersectionFieldsAndAbsentSpeedLimitList() throws IOException {
        ObjectNode root = mapFixture();
        ObjectNode intersection = firstIntersection(root);
        ((ObjectNode) intersection.get("id")).remove("region");
        ((ObjectNode) intersection.get("refPoint")).remove("elevation");
        intersection.remove("laneWidth");
        intersection.remove("speedLimits");

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertContains(messages, "intersections 'id.region' DE_RoadRegulatorID is missing");
        assertContains(messages, "intersections 'refPoint.elevation' DE_Elevation is missing");
        assertContains(messages, "intersections 'laneWidth' DE_LaneWidth is missing");
        assertContains(messages, "intersections 'speedLimits' DF_SpeedLimitList is missing");
    }

    @Test
    void reportsMissingFieldsInPresentSpeedLimitsOncePerField() throws IOException {
        ObjectNode root = mapFixture();
        ArrayNode speedLimits = DateJsonMapper.getInstance().createArrayNode();
        speedLimits.addObject();
        speedLimits.addObject();
        firstIntersection(root).set("speedLimits", speedLimits);

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertEquals(1, countContaining(messages, "speedLimits 'type' DE_SpeedLimitType is missing"));
        assertEquals(1, countContaining(messages, "speedLimits 'speed' DE_Velocity is missing"));
    }

    @Test
    void reportsMissingCoordinatesForEveryPresentNodeDeltaChoice() throws IOException {
        ObjectNode root = mapFixture();
        Set<String> choicesFound = removeCoordinatesFromEachNodeChoice(root);

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertEquals(NODE_XY_CHOICES.length, choicesFound.size(), "The MAP fixture should exercise each NodeXY choice");
        for (String choice : NODE_XY_CHOICES) {
            assertTrue(choicesFound.contains(choice), "The MAP fixture should include " + choice);
            assertContains(messages, "delta." + choice + ".x");
            assertContains(messages, "delta." + choice + ".y");
        }
    }

    @Test
    void reportsComputedNodeListRequirements() throws IOException {
        ObjectNode root = mapFixture();
        ObjectNode nodeList = (ObjectNode) firstLane(root).get("nodeList");
        nodeList.remove("nodes");
        nodeList.set("computed", DateJsonMapper.getInstance().createObjectNode());

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertContains(messages, "computed 'referenceLaneId' DE_LaneID is missing");
        assertContains(messages, "computed 'offsetXaxis'");
        assertContains(messages, "computed 'offsetYaxis'");
    }

    @Test
    void reportsMissingManeuversAndRequiredConnectionForIngressVehicleLane() throws IOException {
        ObjectNode root = mapFixture();
        ObjectNode lane = firstLane(root);
        lane.remove("maneuvers");
        lane.remove("connectsTo");

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertContains(messages, "laneSet 'maneuvers' DE_AllowedManeuvers is missing");
        assertContains(messages, "laneSet 'connectsTo' DF_ConnectsToList is missing for lane ID 18");
    }

    @Test
    void reportsMissingNestedConnectingLaneAndSignalGroupFields() throws IOException {
        ObjectNode root = mapFixture();
        ObjectNode lane = firstLane(root);
        ObjectNode connection = (ObjectNode) ((ArrayNode) lane.get("connectsTo")).get(0);
        ObjectNode connectingLane = (ObjectNode) connection.get("connectingLane");
        connectingLane.remove("lane");
        connectingLane.remove("maneuver");
        connection.remove("signalGroup");

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertContains(messages, "connectsTo 'connectingLane.lane' DE_LaneID is missing");
        assertContains(messages, "connectsTo 'connectingLane.maneuver' DE_AllowedManeuver is missing");
        assertContains(messages, "connectsTo 'signalGroup' DE_SignalGroupID is missing");
    }

    @Test
    void reportsLaneDataAttributeSpeedLimitRequirements() throws IOException {
        ObjectNode root = mapFixture();
        ArrayNode nodes = (ArrayNode) firstLane(root).path("nodeList").path("nodes");
        ObjectNode firstNode = (ObjectNode) nodes.get(0);
        ObjectNode attributes = DateJsonMapper.getInstance().createObjectNode();
        attributes.putArray("data").addObject().putArray("speedLimits").addObject();
        firstNode.set("attributes", attributes);

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertContains(messages, "attributes 'data.speedLimits.type' DE_SpeedLimitType is missing");
        assertContains(messages, "attributes 'data.speedLimits.speed' DE_Velocity is missing");
    }

    @Test
    void reportsMissingSpeedLimitListOnPresentLaneDataAttribute() throws IOException {
        ObjectNode root = mapFixture();
        ArrayNode nodes = (ArrayNode) firstLane(root).path("nodeList").path("nodes");
        ObjectNode firstNode = (ObjectNode) nodes.get(0);
        ObjectNode attributes = DateJsonMapper.getInstance().createObjectNode();
        attributes.putArray("data").addObject();
        firstNode.set("attributes", attributes);

        List<String> messages = messages(CTI4501Validator.mapValidation(mapFrame(root)));

        assertContains(messages, "attributes 'data.speedLimits' DF_SpeedLimitList is missing but "
                + "'attributes.data' DF_LaneDataAttributeList is present");
    }

    private static SPAT spatFrame() throws IOException {
        return spatFrame(spatFixture());
    }

    private static ObjectNode spatFixture() throws IOException {
        return (ObjectNode) DateJsonMapper.getInstance().readTree(loadResource("classpath:json/valid.spat.json"));
    }

    private static SPAT spatFrame(ObjectNode root) throws IOException {
        byte[] bytes = DateJsonMapper.getInstance().writeValueAsBytes(root);
        try (JsonDeserializer<OdeMessageFrameData> deserializer = new JsonDeserializer<>(OdeMessageFrameData.class)) {
            OdeMessageFrameData message = deserializer.deserialize("test-topic", bytes);
            return ((SPATMessageFrame) message.getPayload().getData()).getValue();
        }
    }

    private static ObjectNode mapFixture() throws IOException {
        return (ObjectNode) DateJsonMapper.getInstance().readTree(loadResource("classpath:json/valid.map.json"));
    }

    private static MapData mapFrame(ObjectNode root) throws IOException {
        byte[] bytes = DateJsonMapper.getInstance().writeValueAsBytes(root);
        try (JsonDeserializer<OdeMessageFrameData> deserializer = new JsonDeserializer<>(OdeMessageFrameData.class)) {
            OdeMessageFrameData message = deserializer.deserialize("test-topic", bytes);
            return ((MapDataMessageFrame) message.getPayload().getData()).getValue();
        }
    }

    private static ObjectNode firstIntersection(ObjectNode root) {
        return (ObjectNode) root.at(MAP_FRAME_PATH + "/intersections/0");
    }

    private static ObjectNode firstLane(ObjectNode root) {
        return (ObjectNode) firstIntersection(root).path("laneSet").get(0);
    }

    private static Set<String> removeCoordinatesFromEachNodeChoice(ObjectNode root) {
        Set<String> found = new HashSet<>();
        ArrayNode lanes = (ArrayNode) firstIntersection(root).path("laneSet");
        for (JsonNode lane : lanes) {
            JsonNode nodes = lane.path("nodeList").path("nodes");
            for (JsonNode node : nodes) {
                JsonNode delta = node.path("delta");
                for (String choice : NODE_XY_CHOICES) {
                    if (!found.contains(choice) && delta.has(choice)) {
                        ObjectNode coordinates = (ObjectNode) delta.get(choice);
                        coordinates.remove("x");
                        coordinates.remove("y");
                        found.add(choice);
                    }
                }
            }
        }
        return found;
    }

    private static List<String> messages(List<ProcessedValidationMessage> validationMessages) {
        List<String> messages = new ArrayList<>();
        for (ProcessedValidationMessage validationMessage : validationMessages) {
            messages.add(validationMessage.getMessage());
        }
        return messages;
    }

    private static void assertContains(List<String> messages, String fragment) {
        assertTrue(contains(messages, fragment), "Expected a validation message containing: " + fragment);
    }

    private static boolean contains(List<String> messages, String fragment) {
        return messages.stream().anyMatch(message -> message.contains(fragment));
    }

    private static long countContaining(List<String> messages, String fragment) {
        return messages.stream().filter(message -> message.contains(fragment)).count();
    }
}
