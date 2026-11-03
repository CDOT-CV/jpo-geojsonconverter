package us.dot.its.jpo.geojsonconverter.converter.tim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.networknt.schema.Error;

import us.dot.its.jpo.geojsonconverter.pojos.tim.ProcessedTim;
import us.dot.its.jpo.geojsonconverter.validator.JsonValidatorResult;
import us.dot.its.jpo.geojsonconverter.validator.TimJsonValidator;

class TimValidationDiagnosticsTest {
    private final TimConverter converter = new TimConverter(new TimGeometryConverter());

    @Test
    void schemaFailuresRetainTheirMessageAndSourceLocations() {
        var result = new TimJsonValidator("classpath:schemas/tim.schema.json").validate("{}");
        assertFalse(result.getValidationMessages().isEmpty());
        var processed = converter.createFailureProcessedTim("existing conversion failure");

        converter.jsonValidation(processed, result);

        assertEquals(result.getValidationMessages().size() + 1, processed.getValidationMessages().size());
        assertEquals("existing conversion failure", processed.getValidationMessages().getFirst().getMessage());
        for (int i = 0; i < result.getValidationMessages().size(); i++) {
            var original = result.getValidationMessages().get(i);
            var converted = processed.getValidationMessages().get(i + 1);
            assertEquals(original.getMessage(), converted.getMessage());
            assertEquals(original.getSchemaLocation().toString(), converted.getSchemaPath());
            assertEquals(original.getEvaluationPath().toString(), converted.getJsonPath());
        }
    }

    @Test
    void schemaFailuresWithoutLocationsStillRetainTheirMessage() {
        var error = mock(Error.class);
        when(error.getMessage()).thenReturn("validation failure without locations");
        var result = new JsonValidatorResult();
        result.addValidationMessages(List.of(error));
        var processed = new ProcessedTim();

        converter.jsonValidation(processed, result);

        assertEquals(1, processed.getValidationMessages().size());
        var converted = processed.getValidationMessages().getFirst();
        assertEquals("validation failure without locations", converted.getMessage());
        assertNull(converted.getSchemaPath());
        assertNull(converted.getJsonPath());
    }
}
