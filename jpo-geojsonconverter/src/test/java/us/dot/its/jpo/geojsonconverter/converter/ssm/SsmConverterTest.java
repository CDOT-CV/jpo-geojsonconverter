package us.dot.its.jpo.geojsonconverter.converter.ssm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import us.dot.its.jpo.asn.j2735.r2024.SignalStatusMessage.SignalStatusMessageMessageFrame;
import us.dot.its.jpo.geojsonconverter.pojos.ssm.ProcessedSsm;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static us.dot.its.jpo.geojsonconverter.TestResourceUtil.loadResource;

@Slf4j
public class SsmConverterTest {

    private final static ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @MethodSource("params")
    public void testProcessSsm(String ssmJson, int expectNumberOfRequests) throws JsonProcessingException {
        SsmConverter ssmConverter = new SsmConverter();
        SignalStatusMessageMessageFrame messageFrame =
                mapper.readValue(ssmJson, SignalStatusMessageMessageFrame.class);
        ProcessedSsm processedSsm = ssmConverter.processSsm(messageFrame);
        assertNotNull(processedSsm);
        assertThat(processedSsm, hasProperty("statusList", notNullValue()));
        assertThat(processedSsm.getStatusList(), hasSize(equalTo(expectNumberOfRequests)));
    }

    public static Stream<Arguments> params() throws IOException {
        final String ssmJson = loadResource("classpath:json/ssm.message-frame.json");
        final String ssmJsonMulti = loadResource("classpath:json/ssm.message-frame.multi.json");
        return Stream.of(
                Arguments.of(ssmJson, 1),
                Arguments.of(ssmJsonMulti, 2)
        );
    }


}
