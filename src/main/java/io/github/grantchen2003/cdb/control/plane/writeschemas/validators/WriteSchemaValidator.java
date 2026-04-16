package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WriteSchemaValidator {

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final EnumValidator enumValidator;
    private final IntervalValidator intervalValidator;
    private final NumberValidator numberValidator;
    private final StringValidator stringValidator;

    public WriteSchemaValidator(
            IntervalValidator intervalValidator,
            EnumValidator enumValidator,
            NumberValidator numberValidator,
            StringValidator stringValidator) {
        this.enumValidator = enumValidator;
        this.intervalValidator = intervalValidator;
        this.numberValidator = numberValidator;
        this.stringValidator = stringValidator;
    }

    public void validate(String writeSchemaJson) {
        final JsonNode root;
        try {
            root = OBJECT_MAPPER.readTree(writeSchemaJson);
        } catch (Exception e) {
            throw new InvalidWriteSchemaException("must be valid JSON");
        }

        final JsonNode types = root.get("types");
        validateTypes(types);


        final JsonNode tables = root.get("tables");
        validateTables(tables, types);
    }

    private void validateTypes(JsonNode types) {
        if (types == null || !types.isObject()) {
            throw new InvalidWriteSchemaException("missing required field 'types'");
        }

        types.fields().forEachRemaining(entry -> {
            final JsonNode variant = entry.getValue().get("variant");
            if (variant == null || !variant.isTextual()) {
                throw new InvalidWriteSchemaException("type '" + entry.getKey() + "' is missing 'variant'");
            }

            switch (variant.asText()) {
                case "text" -> {
                    final JsonNode charset = entry.getValue().get("charset");
                    if (charset != null) {
                        stringValidator.validate(charset, List.of("ascii", "unicode"));
                    }

                    final JsonNode size = entry.getValue().get("size");
                    if (size != null) {
                        intervalValidator.validate(size, 0, Integer.MAX_VALUE);
                    }
                }

                case "integer" -> {
                    final JsonNode range = entry.getValue().get("range");
                    if (range != null) {
                        intervalValidator.validate(range, Long.MIN_VALUE, Long.MAX_VALUE);
                    }
                }

                case "decimal" -> {
                    final JsonNode precision = entry.getValue().get("precision");
                    numberValidator.validate(precision, 0, 64);

                    final JsonNode scale = entry.getValue().get("scale");
                    if (scale != null) {
                        intervalValidator.validate(scale, 0, 64);
                    }

                    final JsonNode values = entry.getValue().get("values");
                    if (values != null) {
                        intervalValidator.validate(values, Double.MIN_VALUE, Double.MAX_VALUE);
                    }
                }

                case "binary" -> {
                    final JsonNode size = entry.getValue().get("size");
                    if (size != null) {
                        intervalValidator.validate(size, 0, Integer.MAX_VALUE);
                    }
                }

                case "enum" -> {
                    final JsonNode values = entry.getValue().get("values");
                    enumValidator.validate(values);
                }

                default -> throw new InvalidWriteSchemaException("type '" + entry.getKey() + "' has invalid variant '" + variant.asText() + "'");
            }
        });
    }

    private void validateTables(JsonNode tables, JsonNode validatedTypes) {
        if (tables == null || !tables.isObject()) {
            throw new InvalidWriteSchemaException("missing required field 'tables'");
        }

        tables.fields().forEachRemaining(entry -> {
            final String tableName = entry.getKey();
            final JsonNode tableDef = entry.getValue();

            final JsonNode attributeTypes = tableDef.get("attributeTypes");
            if (attributeTypes == null || !attributeTypes.isObject()) {
                throw new InvalidWriteSchemaException("table '" + tableName + "' is missing 'attributeTypes'");
            }

            attributeTypes.fields().forEachRemaining(attrEntry -> {
                final String referencedType = attrEntry.getValue().asText();
                if (!validatedTypes.has(referencedType)) {
                    throw new InvalidWriteSchemaException("table '" + tableName + "' attribute '" + attrEntry.getKey() + "' references undefined type '" + referencedType + "'");
                }
            });

            final JsonNode primaryKey = tableDef.get("primaryKey");
            if (primaryKey == null || !primaryKey.isArray() || primaryKey.isEmpty()) {
                throw new InvalidWriteSchemaException("table '" + tableName + "' is missing 'primaryKey'");
            }

            primaryKey.forEach(pkField -> {
                final String fieldName = pkField.asText();
                if (!attributeTypes.has(fieldName)) {
                    throw new InvalidWriteSchemaException("table '" + tableName + "' primaryKey field '" + fieldName + "' not found in attributeTypes");
                }
            });

            final JsonNode requiredAttributes = tableDef.get("requiredAttributes");
            if (!requiredAttributes.isArray()) {
                throw new InvalidWriteSchemaException("table '" + tableName + "' field 'requiredAttributes' must be an array");
            }

            requiredAttributes.forEach(reqAttr -> {
                final String fieldName = reqAttr.asText();
                if (!attributeTypes.has(fieldName)) {
                    throw new InvalidWriteSchemaException(
                            "table '" + tableName + "' requiredAttribute '" + fieldName + "' not found in attributeTypes"
                    );
                }
            });

            final JsonNode queryFamilies = tableDef.get("queryFamilies");
            if (queryFamilies != null && queryFamilies.isArray()) {
                queryFamilies.forEach(family -> {
                    if (!family.isArray()) {
                        throw new InvalidWriteSchemaException("Query family must be an array of attributes");
                    }

                    if (family.isEmpty()) {
                        throw new InvalidWriteSchemaException("table '" + tableName + "' has an empty query family; at least one attribute is required");
                    }

                    family.forEach(attr -> {
                        final String fieldName = attr.asText();
                        if (!attributeTypes.has(fieldName)) {
                            throw new InvalidWriteSchemaException("Query family attribute '" + fieldName + "' not found in attributeTypes");
                        }
                    });
                });
            }
        });
    }
}
