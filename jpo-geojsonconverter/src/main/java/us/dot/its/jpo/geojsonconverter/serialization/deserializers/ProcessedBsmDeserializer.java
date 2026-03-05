package us.dot.its.jpo.geojsonconverter.serialization.deserializers;

import java.io.IOException;

import tools.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tools.jackson.databind.JavaType;
import us.dot.its.jpo.geojsonconverter.DateJsonMapper;
import us.dot.its.jpo.geojsonconverter.pojos.geojson.bsm.ProcessedBsm;

public class ProcessedBsmDeserializer<T> implements Deserializer<ProcessedBsm<T>> {
    private static Logger logger = LoggerFactory.getLogger(ProcessedBsmDeserializer.class);

    private final ObjectMapper mapper = DateJsonMapper.getInstance();

    private Class<T> geometryClass;

    public ProcessedBsmDeserializer(Class<T> geometryClass) {
        this.geometryClass = geometryClass;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ProcessedBsm<T> deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            JavaType javaType = mapper.getTypeFactory().constructParametricType(ProcessedBsm.class, geometryClass);
            return (ProcessedBsm<T>) mapper.readValue(data, javaType);
        } catch (IOException e) {
            String errMsg = "Exception deserializing for topic %s: %s".formatted(topic, e.getMessage());
            logger.error(errMsg, e);
            throw new RuntimeException(errMsg, e);
        }
    }
}
