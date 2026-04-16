package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.springframework.stereotype.Component;

@Component
public class NumberValidator {
    public void validate(JsonNode node, long min, long max) {
        if (node == null || !node.isNumber()) {
            throw new InvalidWriteSchemaException("Value must be a numeric type");
        }

        final long value = node.asLong();

        if (value < min || value > max) {
            throw new InvalidWriteSchemaException(
                    String.format("Value %d is out of bounds [%d, %d]", value, min, max)
            );
        }
    }
}
