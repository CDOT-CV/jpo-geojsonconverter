package us.dot.its.jpo.geojsonconverter.validator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Test;

public class JsonValidatorResultTest {
    
    @Test
    public void testAddException() {
        var result = new JsonValidatorResult();
        var ex = new Exception();
        result.addException(ex);
        assertThat(result.getExceptions(), hasItem(equalTo(ex)));
        assertTrue(result.describeResults().contains("Exception"));
        assertFalse(result.isValid());
    }

    
}
