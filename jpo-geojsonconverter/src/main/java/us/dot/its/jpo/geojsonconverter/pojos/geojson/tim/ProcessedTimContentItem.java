package us.dot.its.jpo.geojsonconverter.pojos.geojson.tim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Generated;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a content item in TIM content that can be either an ITIS code or plain text.
 * <p>
 * type - The type of content item (ITIS_CODE or PLAIN_TEXT)
 * <p>
 * itisCode - The ITIS code value (null for plain text items)
 * <p>
 * itisPhrase - The text content (ITIS phrase for ITIS codes, plain text for text items)
 */
@Data
@Generated
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
public class ProcessedTimContentItem {
    private TimContentItemType type;
    private Long itisCode;
    private String itisPhrase;

    public enum TimContentItemType {
        ITIS_CODE, PLAIN_TEXT
    }

    // Convenience constructor for ITIS code items
    public ProcessedTimContentItem(Long itisCode, String itisPhrase) {
        this.type = TimContentItemType.ITIS_CODE;
        this.itisCode = itisCode;
        this.itisPhrase = itisPhrase;
    }

    // Convenience constructor for plain text items
    public static ProcessedTimContentItem createPlainTextItem(String text) {
        ProcessedTimContentItem item = new ProcessedTimContentItem();
        item.type = TimContentItemType.PLAIN_TEXT;
        item.itisCode = null;
        item.itisPhrase = text;
        return item;
    }
}
