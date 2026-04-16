package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.springframework.stereotype.Component;

@Component
public class EnumValidator {
    public void validate(JsonNode values) {
        if (values == null || !values.isArray()) {
            throw new InvalidWriteSchemaException("Enum 'values' must be a JSON array");
        }

        if (values.isEmpty()) {
            throw new InvalidWriteSchemaException("Enum 'values' array cannot be empty");
        }

        for (int i = 0; i < values.size(); i++) {
            final JsonNode element = values.get(i);
            if (!element.isTextual() && !element.isNumber()) {
                throw new InvalidWriteSchemaException(
                        String.format("Invalid enum value at index %d: must be a string or a number", i)
                );
            }
        }
    }
}