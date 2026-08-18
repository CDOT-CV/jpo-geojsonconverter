package us.dot.its.jpo.geojsonconverter.converter.tim;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Component;
import com.networknt.schema.Error;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.Common.HeadingSlice;
import us.dot.its.jpo.asn.j2735.r2024.Common.MinuteOfTheYear;
import us.dot.its.jpo.asn.j2735.r2024.Common.Position3D;
import us.dot.its.jpo.asn.j2735.r2024.J2540ITIS.ITIScodes;
import us.dot.its.jpo.asn.j2735.r2024.ITIS.ITIScodesAndTextSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.Circle;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DirectionOfUse;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.DistanceUnits;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.GeographicalPath;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.SpeedLimitSequence;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerDataFrame.ContentChoice;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInfoType;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.TravelerInformation;
import us.dot.its.jpo.asn.j2735.r2024.TravelerInformation.WorkZoneSequence;
import us.dot.its.jpo.geojsonconverter.converter.FieldConversions;
import us.dot.its.jpo.geojsonconverter.pojos.ProcessedValidationMessage;
import us.dot.its.jpo.geojsonconverter.pojos.common.Ieee1609Dot2SignedDataMetadata;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.Geometry;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.tim.*;
import us.dot.its.jpo.geojsonconverter.pojos.tim.OffsetInformation;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTimCompliance;
import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.utils.J2735DateTimeConverter;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.ode.model.OdeMessageFrameMetadata;

/**
 * Converts ASN.1 TravelerInformation to ProcessedTim objects. This class contains the core conversion logic separated
 * from Kafka Streams specific code.
 */
@Slf4j
@Component
public class TimConverter {

    // Constants
    private static final String UTC_ZONE_ID = "UTC";
    private static final int INFINITE_DURATION_VALUE = 32000;
    private static final ZonedDateTime INFINITE_VALIDITY_PERIOD =
            ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 0, ZoneId.of(UTC_ZONE_ID));

    private final TimGeometryConverter geometryProcessor;

    public TimConverter(TimGeometryConverter geometryProcessor) {
        this.geometryProcessor = geometryProcessor;
    }

    /**
     * Create a processed TIM object from ASN.1 data.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @param metadata The ODE message frame metadata
     * @return Processed TIM object
     */
    public ProcessedTim createProcessedTim(TravelerInformation travelerInfo, OdeMessageFrameMetadata metadata) {
        return createProcessedTim(travelerInfo, metadata, null);
    }

    /**
     * Create a processed TIM object from ASN.1 data and optional IEEE 1609.2
     * signed-message metadata.
     *
     * @param travelerInfo The ASN.1 TravelerInformation object
     * @param metadata The ODE message frame metadata
     * @param signedDataMetadata IEEE 1609.2 signed-data metadata from the raw ODE frame
     * @return Processed TIM object
     */
    public ProcessedTim createProcessedTim(TravelerInformation travelerInfo, OdeMessageFrameMetadata metadata,
            Ieee1609Dot2SignedDataMetadata signedDataMetadata) {
        ZonedDateTime odeDate = Instant.parse(metadata.getOdeReceivedAt()).atZone(ZoneId.of(UTC_ZONE_ID));
        ProcessedTim processedTim = initializeProcessedTim(metadata, travelerInfo, odeDate, signedDataMetadata);

        setComplianceInformation(processedTim);
        setBasicTimProperties(processedTim, travelerInfo);
        setFeatureCollection(processedTim, travelerInfo, odeDate);
        setLocation(processedTim, travelerInfo);

        return processedTim;
    }

    /**
     * Create a failure ProcessedTim object for validation failures.
     *
     * @param message The failure message
     * @return ProcessedTim object indicating failure
     */
    public ProcessedTim createFailureProcessedTim(String message) {
        ProcessedTim processedTim = new ProcessedTim();

        setFailureCompliance(processedTim);
        processedTim.setTimeStamp(ZonedDateTime.now(ZoneOffset.UTC));

        return processedTim;
    }

    /**
     * Initialize the basic ProcessedTim object with metadata and timestamp.
     */
    private ProcessedTim initializeProcessedTim(OdeMessageFrameMetadata metadata, TravelerInformation travelerInfo,
            ZonedDateTime odeDate, Ieee1609Dot2SignedDataMetadata signedDataMetadata) {
        ProcessedTim processedTim = new ProcessedTim();
        processedTim.setOdeReceivedAt(metadata.getOdeReceivedAt());
        processedTim.setOriginIp(metadata.getOriginIp());
        processedTim.setAsn1(metadata.getAsn1());
        processedTim.setSignedDataMetadata(signedDataMetadata);
        processedTim.setCertPresent(metadata.isCertPresent());

        ZonedDateTime creationTimestamp =
                J2735DateTimeConverter.generateUTCTimestamp(travelerInfo.getTimeStamp(), odeDate);
        processedTim.setTimeStamp(creationTimestamp);

        return processedTim;
    }

    /**
     * Set compliance information for the processed TIM.
     */
    private void setComplianceInformation(ProcessedTim processedTim) {
        // Initialize structural ITWG compliance. jsonValidation(...) records schema failures below.
        // TODO: Add content-level ITWG/CTW best-practice validation in the planned TIM validator.
        List<ProcessedTimCompliance> complianceList = new ArrayList<>();
        ProcessedTimCompliance compliance = new ProcessedTimCompliance();
        compliance.setStandard(ProcessedTimCompliance.Standard.ITWG);
        compliance.setCompliant(true);
        compliance.setValidationMessages(new ArrayList<>());
        complianceList.add(compliance);
        processedTim.setCompliance(complianceList);
    }

    /**
     * Set basic TIM properties from ASN.1 object.
     */
    private void setBasicTimProperties(ProcessedTim processedTim, TravelerInformation travelerInfo) {
        if (travelerInfo.getMsgCnt() != null) {
            processedTim.setMsgCnt((int) travelerInfo.getMsgCnt().getValue());
        }

        if (travelerInfo.getPacketID() != null) {
            String packetId = travelerInfo.getPacketID().getValue();
            processedTim.setPacketId(packetId);
        }
    }

    /**
     * Set the feature collection for the processed TIM.
     */
    private void setFeatureCollection(ProcessedTim processedTim, TravelerInformation travelerInfo,
            ZonedDateTime odeDate) {
        try {
            ProcessedTimFeatureCollection featureCollection = new ProcessedTimFeatureCollection();
            List<ProcessedTimFeature<?>> features = processDataFrames(travelerInfo, odeDate);
            featureCollection.setFeatures(features);
            processedTim.setDataFrameFeatureCollection(featureCollection);
        } catch (Exception e) {
            log.error("Error processing TIM ASN.1 data: {}", e.getMessage(), e);
            // Create empty feature collection if processing fails
            ProcessedTimFeatureCollection featureCollection = new ProcessedTimFeatureCollection();
            featureCollection.setFeatures(new ArrayList<ProcessedTimFeature<?>>());
            processedTim.setDataFrameFeatureCollection(featureCollection);
        }
    }

    /**
     * Set the representative region-anchor location used for coarse MongoDB 2dsphere indexing.
     */
    private void setLocation(ProcessedTim processedTim, TravelerInformation travelerInfo) {
        try {
            Point jtsPoint = geometryProcessor.calculateCenterLocationFromRegionAnchors(travelerInfo);
            if (jtsPoint != null) {
                // Convert JTS Point to GeoJSON Point
                us.dot.its.jpo.geojsonconverter.pojos.geojson.Point geoJsonPoint =
                        new us.dot.its.jpo.geojsonconverter.pojos.geojson.Point(jtsPoint.getX(), jtsPoint.getY());
                processedTim.setLocation(geoJsonPoint);
            } else {
                log.warn("Omitting TIM location because no representative region-anchor point could be calculated; packetId={}",
                        processedTim.getPacketId());
            }
        } catch (Exception e) {
            log.error("Error calculating center location: {}", e.getMessage(), e);
            // Location will remain null if calculation fails
        }
    }

    /**
     * Set compliance information for failure cases.
     */
    private void setFailureCompliance(ProcessedTim processedTim) {
        List<ProcessedTimCompliance> complianceList = new ArrayList<>();
        ProcessedTimCompliance compliance = new ProcessedTimCompliance();
        compliance.setStandard(ProcessedTimCompliance.Standard.ITWG);
        compliance.setCompliant(false);
        compliance.setValidationMessages(new ArrayList<>());
        complianceList.add(compliance);
        processedTim.setCompliance(complianceList);
    }

    /**
     * Process data frames from the traveler information.
     */
    private List<ProcessedTimFeature<?>> processDataFrames(TravelerInformation travelerInfo, ZonedDateTime odeDate) {
        List<ProcessedTimFeature<?>> features = new ArrayList<>();

        if (travelerInfo.getDataFrames() != null) {
            for (int i = 0; i < travelerInfo.getDataFrames().size(); i++) {
                TravelerDataFrame dataFrame = travelerInfo.getDataFrames().get(i);
                ProcessedTimFeature<?> feature = createProcessedTimFeatureFromAsnData(odeDate, dataFrame, i);
                if (feature != null) {
                    features.add(feature);
                }
            }
        }

        return features;
    }

    /**
     * Create a processed TIM feature from ASN.1 data.
     */
    private ProcessedTimFeature<?> createProcessedTimFeatureFromAsnData(ZonedDateTime odeDate,
            TravelerDataFrame dataFrame, int featureId) {
        try {
            ProcessedTimProperties properties = createProcessedTimProperties(dataFrame, odeDate);
            Geometry geometry = geometryProcessor.createGeometryFromDataFrame(dataFrame);

            return new ProcessedTimFeature<>(featureId, geometry, properties);
        } catch (Exception e) {
            log.error("Error creating TIM feature from ASN.1 data: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Create processed TIM properties from ASN.1 data.
     */
    private ProcessedTimProperties createProcessedTimProperties(TravelerDataFrame dataFrame, ZonedDateTime odeDate) {
        ProcessedTimProperties properties = new ProcessedTimProperties();

        setDeploymentAgency(properties, dataFrame);
        setValidityPeriod(properties, dataFrame, odeDate);
        setPriority(properties, dataFrame);
        setRegionAndDirectionInfo(properties, dataFrame);
        setContent(properties, dataFrame);

        return properties;
    }

    /**
     * Set deployment agency based on frame type.
     */
    private void setDeploymentAgency(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getFrameType() != null) {
            TravelerInfoType frameType = dataFrame.getFrameType();
            ProcessedDeploymentAgency agency = ProcessedDeploymentAgency.fromValue(frameType);
            properties.setDeploymentAgencyType(agency);
        }
    }

    /**
     * Set validity period for the TIM feature.
     */
    private void setValidityPeriod(ProcessedTimProperties properties, TravelerDataFrame dataFrame,
            ZonedDateTime odeDate) {
        ProcessedValidityPeriod validityPeriod = new ProcessedValidityPeriod();
        ZonedDateTime startDateTime = odeDate;

        // ASN.1 startYear is optional; when absent, use the ODE receive year with MinuteOfTheYear
        if (dataFrame.getStartTime() != null) {
            Integer startYear =
                    dataFrame.getStartYear() != null ? (int) dataFrame.getStartYear().getValue() : null;
            MinuteOfTheYear startTimeMoy = dataFrame.getStartTime();
            startDateTime = J2735DateTimeConverter.generateUTCTimestamp(startTimeMoy, null, odeDate, startYear);
        }
        validityPeriod.setStartTime(startDateTime);

        if (dataFrame.getDurationTime() != null) {
            int duration = (int) dataFrame.getDurationTime().getValue();

            if (duration != INFINITE_DURATION_VALUE) {
                validityPeriod.setInfinite(false);
                validityPeriod.setEndTime(startDateTime.plusMinutes(duration));
            } else {
                validityPeriod.setInfinite(true);
                validityPeriod.setEndTime(INFINITE_VALIDITY_PERIOD);
            }
        } else {
            validityPeriod.setInfinite(true);
        }

        properties.setValidityPeriod(validityPeriod);
    }

    /**
     * Set priority from data frame.
     */
    private void setPriority(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getPriority() != null) {
            properties.setPriority((int) dataFrame.getPriority().getValue());
        }
    }

    /**
     * Set region and direction information.
     */
    private void setRegionAndDirectionInfo(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getRegions() != null && !dataFrame.getRegions().isEmpty()) {
            List<ProcessedRegionInfoBase> regionInfoList = new ArrayList<>();

            // Create one region info object for each region
            for (GeographicalPath region : dataFrame.getRegions()) {
                ProcessedRegionInfoBase regionInfo = createProcessedRegionInfoFromAsnData(region);
                regionInfoList.add(regionInfo);
            }

            properties.setRegionInfoList(regionInfoList);
        }
    }

    /**
     * Set content information.
     */
    private void setContent(ProcessedTimProperties properties, TravelerDataFrame dataFrame) {
        if (dataFrame.getContent() != null) {
            ProcessedTimContent content = createProcessedTimContentFromAsnData(dataFrame.getContent());
            properties.setContent(content);
        }
    }

    /**
     * Create processed region info from ASN.1 data.
     */
    private ProcessedRegionInfoBase createProcessedRegionInfoFromAsnData(GeographicalPath region) {
        ProcessedRegionType regionType = determineRegionType(region);
        ProcessedElevationProfile elevationProfile = new ProcessedElevationProfile();

        // Set anchor point and elevation from anchor
        ProcessedAnchorPoint anchorPoint = setAnchorPointAndElevation(region, elevationProfile);

        // Extract and apply offset information for elevation and lane width profiles
        populateProfilesWithOffsets(region, elevationProfile);

        // Create the appropriate region info object based on type
        ProcessedRegionInfoBase regionInfo = createRegionInfoByType(regionType, region, elevationProfile);
        regionInfo.setAnchorPoint(anchorPoint);

        // Set direction info for this specific region (ITWG rules vary by geofence type)
        ProcessedDirectionInfoBase directionInfo = createProcessedDirectionInfoFromAsnData(region, regionType);
        regionInfo.setDirectionInfo(directionInfo);

        return regionInfo;
    }

    /**
     * Populate elevation and lane width profiles with offset-calculated values.
     */
    private void populateProfilesWithOffsets(GeographicalPath region, ProcessedElevationProfile elevationProfile) {
        // Extract offset information from the region's path
        OffsetInformation offsetInfo = geometryProcessor.extractOffsetInformation(region);
        if (offsetInfo == null) {
            return;
        }

        List<Long> elevationOffsets = offsetInfo.getElevationOffsets();

        // Populate elevation profile with offset-calculated values
        if (elevationOffsets != null && !elevationOffsets.isEmpty()) {
            List<Double> nodeElevationMeters = new ArrayList<>(elevationOffsets.size());
            Double currentElevation = elevationProfile.getDefaultElevationMeters();

            for (Long offset : elevationOffsets) {
                if (currentElevation != null && offset != null) {
                    currentElevation = FieldConversions.calculateElevationOffset(currentElevation, offset);
                }
                nodeElevationMeters.add(currentElevation);
            }

            elevationProfile.setNodeElevationMeters(nodeElevationMeters);
        }
    }

    /**
     * Populate lane width profile with offset-calculated values.
     */
    private void populateLaneWidthProfileWithOffsets(GeographicalPath region,
            ProcessedLaneWidthProfile laneWidthProfile) {
        // Extract offset information from the region's path
        OffsetInformation offsetInfo = geometryProcessor.extractOffsetInformation(region);
        if (offsetInfo == null) {
            return;
        }

        List<Long> laneWidthOffsets = offsetInfo.getLaneWidthOffsets();

        // Populate lane width profile with offset-calculated values
        if (laneWidthOffsets != null && !laneWidthOffsets.isEmpty()) {
            List<Double> nodeLaneWidthMeters = new ArrayList<>(laneWidthOffsets.size());
            Double currentWidth = laneWidthProfile.getDefaultWidthMeters();

            for (Long offset : laneWidthOffsets) {
                if (currentWidth != null && offset != null) {
                    currentWidth = FieldConversions.calculateLaneWidthOffset(currentWidth, offset);
                }
                nodeLaneWidthMeters.add(currentWidth);
            }

            laneWidthProfile.setNodeLaneWidthMeters(nodeLaneWidthMeters);
        }
    }

    /**
     * Create processed TIM content from ASN.1 data.
     */
    private ProcessedTimContent createProcessedTimContentFromAsnData(ContentChoice content) {
        ProcessedTimContent timContent = new ProcessedTimContent();
        List<ProcessedTimContentItem> contentItems = new ArrayList<>();

        if (content != null) {
            if (content.getAdvisory() != null) {
                timContent.setType(ProcessedContentType.ADVISORY);
                processAdvisoryContent(content.getAdvisory(), contentItems);
            } else if (content.getSpeedLimit() != null) {
                timContent.setType(ProcessedContentType.ROAD_SIGNAGE);
                processSpeedLimitContent(content.getSpeedLimit(), contentItems);
            } else if (content.getWorkZone() != null) {
                timContent.setType(ProcessedContentType.COMMERCIAL_SIGNAGE);
                processWorkZoneContent(content.getWorkZone(), contentItems);
            }
        }

        // Set the ordered content items
        timContent.setContentItems(contentItems);

        // Create combined message from content items in order
        String combinedMessage = contentItems.stream().map(ProcessedTimContentItem::getItisPhrase)
                .filter(text -> text != null && !text.trim().isEmpty()).collect(Collectors.joining(" "));
        timContent.setSentence(combinedMessage);

        return timContent;
    }

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

    private ProcessedAnchorPoint setAnchorPointAndElevation(GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        if (region.getAnchor() == null) {
            return null;
        }

        Position3D anchor = region.getAnchor();
        ProcessedAnchorPoint anchorPoint = new ProcessedAnchorPoint();

        // Set latitude and longitude
        if (anchor.getLat() != null) {
            anchorPoint.setLatitude(FieldConversions.convertLat(anchor.getLat().getValue()));
        }
        if (anchor.getLong_() != null) {
            anchorPoint.setLongitude(FieldConversions.convertLong(anchor.getLong_().getValue()));
        }
        if (anchor.getElevation() != null) {
            anchorPoint.setElevationMeters(FieldConversions.convertElevation(anchor.getElevation().getValue()));
        }

        // Set default elevation from anchor point
        if (anchor.getElevation() != null) {
            elevationProfile
                    .setDefaultElevationMeters(FieldConversions.convertElevation(anchor.getElevation().getValue()));
        }

        return anchorPoint;
    }

    private ProcessedRegionInfoBase createRegionInfoByType(ProcessedRegionType regionType, GeographicalPath region,
            ProcessedElevationProfile elevationProfile) {
        ProcessedRegionInfoBase regionInfo;

        switch (regionType) {
            case PATH:
                regionInfo = new ProcessedPathRegionInfo();
                // Set lane width profile for path regions
                if (region.getLaneWidth() != null) {
                    ProcessedLaneWidthProfile laneWidthProfile = new ProcessedLaneWidthProfile();
                    laneWidthProfile.setDefaultWidthMeters(FieldConversions.convertLaneWidth(region.getLaneWidth()));

                    // Populate lane width profile with offset-calculated values
                    populateLaneWidthProfileWithOffsets(region, laneWidthProfile);

                    ((ProcessedPathRegionInfo) regionInfo).setLaneWidthProfile(laneWidthProfile);
                }
                break;
            case CIRCLE:
                regionInfo = new ProcessedCircleRegionInfo();
                if (region.getDescription() != null && region.getDescription().getGeometry() != null
                        && region.getDescription().getGeometry().getCircle() != null) {
                    Circle circle = region.getDescription().getGeometry().getCircle();
                    if (circle.getRadius() != null) {
                        long radiusValue = circle.getRadius().getValue();
                        DistanceUnits units = circle.getUnits();
                        Double radiusMeters = FieldConversions.convertDistanceToMeters(radiusValue, units);
                        if (radiusMeters != null) {
                            ((ProcessedCircleRegionInfo) regionInfo).setRadius(radiusMeters);
                        }
                    }
                }
                break;
            case POLYGON:
                regionInfo = new ProcessedPolygonRegionInfo();
                break;
            case UNKNOWN:
            default:
                regionInfo = new ProcessedUnknownRegionInfo();
                break;
        }

        // Set common properties
        regionInfo.setRegionType(regionType);
        regionInfo.setElevationProfile(elevationProfile);

        return regionInfo;
    }

    /**
     * Build direction info per ITWG TIM best practices:
     * <ul>
     * <li>PATH ({@code closedPath=false}): use {@code GeographicalPath.directionality}</li>
     * <li>POLYGON ({@code closedPath=true}): use {@code GeographicalPath.direction} (HeadingSlice)</li>
     * <li>CIRCLE: use {@code description.geometry.direction} (HeadingSlice), not path directionality</li>
     * </ul>
     * Non-applicable direction fields are ignored so polygon/circle features are not dropped when a
     * non-compliant {@code directionality} is present.
     */
    private ProcessedDirectionInfoBase createProcessedDirectionInfoFromAsnData(GeographicalPath region,
            ProcessedRegionType regionType) {
        return switch (regionType) {
            case PATH -> {
                if (region.getDirectionality() != null) {
                    yield createDirectionalityDirectionInfo(region.getDirectionality());
                }
                // Non-preferred fallback for deployments that encode path heading via HeadingSlice
                yield createHeadingDirectionInfo(region.getDirection());
            }
            case POLYGON -> createHeadingDirectionInfo(region.getDirection());
            case CIRCLE -> {
                HeadingSlice geometryDirection = null;
                if (region.getDescription() != null && region.getDescription().getGeometry() != null) {
                    geometryDirection = region.getDescription().getGeometry().getDirection();
                }
                yield createHeadingDirectionInfo(geometryDirection);
            }
            case UNKNOWN -> {
                if (region.getDirectionality() != null) {
                    yield createDirectionalityDirectionInfo(region.getDirectionality());
                }
                yield createHeadingDirectionInfo(region.getDirection());
            }
        };
    }

    private ProcessedDirectionalityDirectionInfo createDirectionalityDirectionInfo(DirectionOfUse directionalityAsn) {
        ProcessedDirectionalityDirectionInfo directionalityInfo = new ProcessedDirectionalityDirectionInfo();
        directionalityInfo.setDirectionType(ProcessedDirectionType.DIRECTIONALITY);

        // Prefer ASN getName() ("forward"); Enum.toString() ("FORWARD") also works after toLowerCase
        String directionalityValue =
                directionalityAsn.getName() != null ? directionalityAsn.getName() : directionalityAsn.toString();
        try {
            ProcessedDirectionality directionality =
                    ProcessedDirectionality.fromValue(directionalityValue.toLowerCase());
            directionalityInfo.setDirectionality(directionality);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown directionality value: {}, using UNKNOWN", directionalityValue);
            directionalityInfo.setDirectionality(ProcessedDirectionality.UNKNOWN);
        }

        return directionalityInfo;
    }

    private ProcessedHeadingDirectionInfo createHeadingDirectionInfo(HeadingSlice directionBitstring) {
        if (directionBitstring == null) {
            return null;
        }

        ProcessedHeadingDirectionInfo headingInfo = new ProcessedHeadingDirectionInfo();
        headingInfo.setDirectionType(ProcessedDirectionType.HEADING);

        int[][] sectorRanges = FieldConversions.parseHeadingSectorsAsRanges(directionBitstring);
        if (sectorRanges.length == 0) {
            return null;
        }

        List<ProcessedHeading> headingList = new ArrayList<>();
        for (int[] range : sectorRanges) {
            ProcessedHeading processedHeading = new ProcessedHeading();
            double[] headingAndRange = FieldConversions.sectorRangeToHeadingAndRange(range[0], range[1]);
            processedHeading.setHeading(headingAndRange[0]);
            processedHeading.setRange(headingAndRange[1]);
            headingList.add(processedHeading);
        }

        headingInfo.setHeadingList(headingList);
        return headingInfo;
    }

    private void processAdvisoryContent(List<ITIScodesAndTextSequence> advisoryList,
            List<ProcessedTimContentItem> contentItems) {
        if (advisoryList == null || advisoryList.isEmpty()) {
            return;
        }

        for (ITIScodesAndTextSequence advisory : advisoryList) {
            // Process ITIS codes - check for item field first
            if (advisory.getItem() != null && advisory.getItem().getItis() != null) {
                Long itisCode = advisory.getItem().getItis().getValue();
                String itisPhrase = lookupItisCode(itisCode);
                contentItems.add(new ProcessedTimContentItem(itisCode, itisPhrase));
            }

            // Process text content if available
            if (advisory.getItem() != null && advisory.getItem().getText() != null) {
                String text = advisory.getItem().getText().getValue();
                if (text != null && !text.trim().isEmpty()) {
                    contentItems.add(ProcessedTimContentItem.createPlainTextItem(text.trim()));
                }
            }
        }
    }

    private void processSpeedLimitContent(List<SpeedLimitSequence> speedLimitList,
            List<ProcessedTimContentItem> contentItems) {
        if (speedLimitList == null || speedLimitList.isEmpty()) {
            return;
        }

        for (SpeedLimitSequence speedLimit : speedLimitList) {
            // Process ITIS codes - check for item field first
            if (speedLimit.getItem() != null && speedLimit.getItem().getItis() != null) {
                Long itisCode = speedLimit.getItem().getItis().getValue();
                String itisPhrase = lookupItisCode(itisCode);
                contentItems.add(new ProcessedTimContentItem(itisCode, itisPhrase));
            }

            // Process text content if available
            if (speedLimit.getItem() != null && speedLimit.getItem().getText() != null) {
                String text = speedLimit.getItem().getText().getValue();
                if (text != null && !text.trim().isEmpty()) {
                    contentItems.add(ProcessedTimContentItem.createPlainTextItem(text.trim()));
                }
            }
        }
    }

    private void processWorkZoneContent(List<WorkZoneSequence> workZoneList,
            List<ProcessedTimContentItem> contentItems) {
        if (workZoneList == null || workZoneList.isEmpty()) {
            return;
        }

        for (WorkZoneSequence workZone : workZoneList) {
            // Process ITIS codes - check for item field first
            if (workZone.getItem() != null && workZone.getItem().getItis() != null) {
                Long itisCode = workZone.getItem().getItis().getValue();
                String itisPhrase = lookupItisCode(itisCode);
                contentItems.add(new ProcessedTimContentItem(itisCode, itisPhrase));
            }

            // Process text content if available
            if (workZone.getItem() != null && workZone.getItem().getText() != null) {
                String text = workZone.getItem().getText().getValue();
                if (text != null && !text.trim().isEmpty()) {
                    contentItems.add(ProcessedTimContentItem.createPlainTextItem(text.trim()));
                }
            }
        }
    }

    /**
     * Look up a single ITIS code and return its human-readable message.
     *
     * @param itisCode The ITIS code to look up
     * @return The human-readable message for the ITIS code, or "unknown" if not found
     */
    public static String lookupItisCode(Long itisCode) {
        if (itisCode == null) {
            return "unknown";
        }

        try {
            ITIScodes itisCodes = new ITIScodes(itisCode);
            return itisCodes.name().orElse("unknown");
        } catch (Exception e) {
            log.debug("Error looking up ITIS code {}: {}", itisCode, e.getMessage());
            return "unknown";
        }
    }

    /**
     * Add JSON schema validation results for J2735 and Metadata validation.
     * 
     * @param processedTim The processed TIM object to add validation messages to
     * @param validatorResult the schema validator result
     */
    public void jsonValidation(ProcessedTim processedTim, JsonValidatorResult validatorResult) {
        if (processedTim.getCompliance() == null || processedTim.getCompliance().isEmpty()) {
            // Initialize compliance if not already set
            setComplianceInformation(processedTim);
        }

        // Get the first compliance object (ITWG standard)
        ProcessedTimCompliance compliance = processedTim.getCompliance().getFirst();

        for (Exception exception : validatorResult.getExceptions()) {
            var msg = new ProcessedValidationMessage();
            msg.setMessage(exception.getMessage());
            msg.setException(Arrays.toString(exception.getStackTrace()));
            compliance.getValidationMessages().add(msg);
        }
        for (Error vm : validatorResult.getValidationMessages()) {
            var msg = new ProcessedValidationMessage();
            msg.setMessage(vm.getMessage());
            final var schemaLocation = vm.getSchemaLocation();
            if (schemaLocation != null) {
                msg.setSchemaPath(schemaLocation.toString());
            } else {
                log.warn("validationMessage.schemaLocation is null");
            }
            final var evaluationPath = vm.getEvaluationPath();
            if (evaluationPath != null) {
                msg.setJsonPath(evaluationPath.toString());
            }
            compliance.getValidationMessages().add(msg);
        }
        compliance.setCompliant(compliance.getValidationMessages().isEmpty());
    }
}
