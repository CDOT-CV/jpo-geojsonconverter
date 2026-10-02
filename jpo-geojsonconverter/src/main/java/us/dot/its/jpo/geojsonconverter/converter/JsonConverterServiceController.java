package us.dot.its.jpo.geojsonconverter.converter;

import org.apache.kafka.streams.Topology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import us.dot.its.jpo.geojsonconverter.GeoJsonConverterProperties;
import us.dot.its.jpo.geojsonconverter.StreamsExceptionHandler;
import us.dot.its.jpo.geojsonconverter.converter.map.MapTopology;
import us.dot.its.jpo.geojsonconverter.converter.psm.PsmTopology;
import us.dot.its.jpo.geojsonconverter.converter.rtcm.RTCMConverter;
import us.dot.its.jpo.geojsonconverter.converter.rtcm.RTCMTopology;
import us.dot.its.jpo.geojsonconverter.converter.spat.SpatTopology;
import us.dot.its.jpo.geojsonconverter.converter.bsm.BsmTopology;
import us.dot.its.jpo.geojsonconverter.converter.tim.TimConverter;
import us.dot.its.jpo.geojsonconverter.converter.tim.TimTopology;
import us.dot.its.jpo.geojsonconverter.converter.srm.SrmConverter;
import us.dot.its.jpo.geojsonconverter.converter.srm.SrmTopology;
import us.dot.its.jpo.geojsonconverter.converter.ssm.SsmConverter;
import us.dot.its.jpo.geojsonconverter.converter.ssm.SsmTopology;
import us.dot.its.jpo.geojsonconverter.validator.*;

/**
 * Launches JsonFromJsonConverter service
 */
@Controller
public class JsonConverterServiceController {

    private static final Logger logger = LoggerFactory.getLogger(JsonConverterServiceController.class);
    org.apache.kafka.common.serialization.Serdes bas;

    @Autowired
    public JsonConverterServiceController(GeoJsonConverterProperties geojsonProps, MapJsonValidator mapJsonValidator,
            SpatJsonValidator spatJsonValidator, BsmJsonValidator bsmJsonValidator, PsmJsonValidator psmJsonValidator,
            RTCMJsonValidator rtcmJsonValidator, RTCMConverter rtcmConverter, TimJsonValidator timJsonValidator,
            TimConverter timConverter, SrmJsonValidator srmJsonValidator, SrmConverter srmConverter,
            SsmJsonValidator ssmJsonValidator, SsmConverter ssmConverter) {
        super();
        var streamsStarter = new KafkaStreamsStarter();

        try {
            logger.debug("Starting {}", this.getClass().getSimpleName());

            // MAP
            logger.info("Creating the Processed MAP Kafka-Streams topology");

            var mapTopology = MapTopology.build(geojsonProps.getKafkaTopicOdeMapJson(),
                    geojsonProps.getKafkaTopicProcessedMap(), geojsonProps.getKafkaTopicProcessedMapWKT(),
                    mapJsonValidator, geojsonProps.getGeometryOutputMode());
            streamsStarter.start(mapTopology, geojsonProps.createStreamProperties("processedmapjson"),
                    new StreamsExceptionHandler("MapStream"));

            // SPaT
            logger.info("Creating the Processed SPaT Kafka-Streams topology");

            var spatTopology = SpatTopology.build(geojsonProps.getKafkaTopicOdeSpatJson(),
                    geojsonProps.getKafkaTopicSpatGeoJson(), spatJsonValidator);
            streamsStarter.start(spatTopology, geojsonProps.createStreamProperties("processedspatjson"),
                    new StreamsExceptionHandler("SpatStream"));

            // BSM
            logger.info("Creating the Processed BSM Kafka-Streams topology");

            var bsmTopology = BsmTopology.build(geojsonProps.getKafkaTopicOdeBsmJson(),
                    geojsonProps.getKafkaTopicProcessedBsm(), bsmJsonValidator);
            streamsStarter.start(bsmTopology, geojsonProps.createStreamProperties("processedbsmjson"),
                    new StreamsExceptionHandler("BsmStream"));

            // PSM
            logger.info("Creating the Processed PSM Kafka-Streams topology");

            var psmTopology = PsmTopology.build(geojsonProps.getKafkaTopicOdePsmJson(),
                    geojsonProps.getKafkaTopicProcessedPsm(), psmJsonValidator);
            streamsStarter.start(psmTopology, geojsonProps.createStreamProperties("processedpsmjson"),
                    new StreamsExceptionHandler("PsmStream"));

            // RTCM
            logger.info("Creating the ProcessedRTCM Kafka Streams topology");
            Topology rtcmTopology = RTCMTopology.build(geojsonProps.getKafkaTopicOdeRtcmJson(),
                    geojsonProps.getKafkaTopicProcessedRtcm(), rtcmJsonValidator, rtcmConverter);
            streamsStarter.start(rtcmTopology, geojsonProps.createStreamProperties("processedrcmjson"),
                    new StreamsExceptionHandler("RTCMStream"));

            // TIM
            logger.info("Creating the Processed TIM Kafka-Streams topology");

            var timTopology = TimTopology.build(geojsonProps.getKafkaTopicOdeTimJson(),
                    geojsonProps.getKafkaTopicProcessedTim(), timJsonValidator, timConverter);
            streamsStarter.start(timTopology, geojsonProps.createStreamProperties("processedtimjson"),
                    new StreamsExceptionHandler("TimStream"));

            // SRM
            logger.info("Creating the ProcessedSrm Kafka Streams topology");
            Topology srmTopology = SrmTopology.build(geojsonProps.getKafkaTopicOdeSrmJson(),
                    geojsonProps.getKafkaTopicProcessedSrm(), srmJsonValidator, srmConverter);
            streamsStarter.start(srmTopology, geojsonProps.createStreamProperties("processedsrmjson"));

            // SSM
            logger.info("Creating the ProcessedSsm Kafka Streams topology");
            Topology ssmTopology = SsmTopology.build(geojsonProps.getKafkaTopicOdeSsmJson(),
                    geojsonProps.getKafkaTopicProcessedSsm(), ssmJsonValidator, ssmConverter);
            streamsStarter.start(ssmTopology, geojsonProps.createStreamProperties("processedssmjson"));

            logger.info("All geoJSON conversion services started!");
        } catch (Exception e) {
            logger.error("Encountered error with creating topologies: ", e);
        }
    }
}
