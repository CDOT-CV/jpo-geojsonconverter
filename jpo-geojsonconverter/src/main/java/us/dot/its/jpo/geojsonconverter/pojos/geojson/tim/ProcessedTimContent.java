package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents the content of a TIM message with support for both ITIS codes and plain text.
 * <p>
 * type - The type of content (advisory, roadSignage, commercialSignage)
 * <p>
 * contentItems - Ordered list of content items (ITIS codes and plain text) as they appear in the original message
 * <p>
 * sentence - Combined message from ITIS phrases and plain text
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedTimContent {
    private ProcessedContentType type;
    private List<ProcessedTimContentItem> contentItems;
    private String sentence;
}
