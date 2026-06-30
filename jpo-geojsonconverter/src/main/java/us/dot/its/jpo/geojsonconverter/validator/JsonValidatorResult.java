package us.dot.its.jpo.geojsonconverter.validator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Formatter;
import java.util.List;

import com.networknt.schema.Error;

public class JsonValidatorResult {

      
    private final List<Exception> exceptions = new ArrayList<>();
    private final List<Error> validationMessages = new ArrayList<>();

    public boolean isValid() {
        return exceptions.isEmpty() && validationMessages.isEmpty();
    }

   
    /**
     * @return Jackson JSON processing exceptions
     */
    public List<Exception> getExceptions() {
        return exceptions;
    }

    public void addException(Exception ex) {
        exceptions.add(ex);
    }

    /**
     * @return ValidationMessages returned by the schema validator.
     */
    public List<Error> getValidationMessages() {
        return validationMessages;
    }

    public void addValidationMessages(Collection<Error> messages) {
        validationMessages.addAll(messages);
        
    }

    
    public String describeResults() {
        var sb = new StringBuilder();
        try (var fmt = new Formatter(sb)) {
            fmt.format("Json Validator result: isValid = %s%n", isValid());

            if (!isValid()) {
                fmt.format("Validation Errors:%n");
                
                for (var exception : getExceptions()) {
                    fmt.format("JsonProcessingException: %s%n", exception.getMessage());
                }

                for (Error vm : getValidationMessages()) {
                    fmt.format("At path %s (Schema: %s)%n", vm.getMessage(), vm.getSchemaLocation());
                }
            }
            fmt.format("%n");
        }
        return sb.toString();
    }

}
