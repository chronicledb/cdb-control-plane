package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StringValidator {
    public void validate(JsonNode stringNode, List<String> validValues) {
        if (stringNode == null || !stringNode.isTextual()) {
            throw new InvalidWriteSchemaException("Value must be a string");
        }

        final String value = stringNode.asText();

        if (!validValues.contains(value)) {
            throw new InvalidWriteSchemaException(
                    "Invalid value '" + value + "'. Must be one of: " + validValues
            );
        }
    }
}
