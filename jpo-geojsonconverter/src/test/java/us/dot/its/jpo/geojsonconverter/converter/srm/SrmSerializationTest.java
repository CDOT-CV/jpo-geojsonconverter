package us.dot.its.jpo.geojsonconverter.converter.srm;

import java.io.IOException;
import java.time.ZonedDateTime;
import org.junit.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import us.dot.its.jpo.asn.j2735.r2024.SignalRequestMessage.SignalRequestMessageMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.srm.ProcessedSrm;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.srm.SrmProperties;
import us.dot.its.jpo.geojsonconverter.utils.ProcessedSchemaVersions;
import static us.dot.its.jpo.geojsonconverter.TestResourceUtil.loadResource;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@Slf4j
public class SrmSerializationTest {

    private final static ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testProcessSrmSerialization() throws IOException {
        final String srmJson = loadResource("classpath:json/srm.message-frame.json");
        final String referenceProcessedSrmJson = loadResource("classpath:json/sample.processed-srm.json");
        SrmConverter srmConverter = new SrmConverter();
        SignalRequestMessageMessageFrame messageFrame =
                mapper.readValue(srmJson, SignalRequestMessageMessageFrame.class);
        ZonedDateTime ingestTime = ZonedDateTime.parse("2025-09-26T00:46:10.771Z");
        ProcessedSrm processedSrm = srmConverter.processSrm(messageFrame, ingestTime);

        SrmProperties properties = processedSrm.getProperties();
        properties.setAsn1(
                "001D2F72DC028000050890BD481080201F1A62242F5205200408430AB02F236614C002D4D3841D02CA71A8851970D51C3060");
        properties.setSchemaVersion(ProcessedSchemaVersions.PROCESSED_SRM_SCHEMA_VERSION);
        properties.setOriginIp("172.18.0.1");
        properties.setOdeReceivedAt(ingestTime);

        assertThatJson(referenceProcessedSrmJson).isEqualTo(processedSrm.toString());
    }
}
