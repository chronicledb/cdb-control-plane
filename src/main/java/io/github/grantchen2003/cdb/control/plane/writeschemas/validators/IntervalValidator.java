package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.springframework.stereotype.Component;

@Component
public class IntervalValidator {
    public void validate(JsonNode interval, double minLowerBound, double maxUpperBound) {
        if (interval == null || !interval.isArray()) {
            throw new InvalidWriteSchemaException("Interval must be a JSON array");
        }

        if (interval.size() != 2) {
            throw new InvalidWriteSchemaException("Interval must contain exactly 2 elements (lower and upper bound)");
        }

        if (!interval.get(0).isNumber() || !interval.get(1).isNumber()) {
            throw new InvalidWriteSchemaException("Interval elements must be numeric");
        }

        final double lowerBound = interval.get(0).asDouble();
        final double upperBound = interval.get(1).asDouble();

        if (lowerBound < minLowerBound) {
            throw new InvalidWriteSchemaException("Lower bound " + lowerBound + " is below minimum allowed: " + minLowerBound);
        }

        if (upperBound > maxUpperBound) {
            throw new InvalidWriteSchemaException("Upper bound " + upperBound + " is above maximum allowed: " + maxUpperBound);
        }

        if (lowerBound > upperBound) {
            throw new InvalidWriteSchemaException("Invalid interval bounds: " + lowerBound + " must be <= " + upperBound);
        }
    }
}