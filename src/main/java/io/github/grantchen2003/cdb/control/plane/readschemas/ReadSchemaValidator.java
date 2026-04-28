package io.github.grantchen2003.cdb.control.plane.readschemas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ReadSchemaValidator {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void validate(String readSchemaJson) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(readSchemaJson);
        } catch (Exception e) {
            throw new InvalidReadSchemaException("must be valid JSON");
        }

        validateTables(root);
    }

    private void validateTables(JsonNode root) {
        final JsonNode tables = root.get("tables");
        if (tables == null || !tables.isObject()) {
            throw new InvalidReadSchemaException("missing required field 'tables' for TRANSACTIONAL schema");
        }

        tables.fields().forEachRemaining(entry -> {
            final String tableName = entry.getKey();
            final JsonNode tableDef = entry.getValue();

            final JsonNode queries = tableDef.get("queries");
            if (queries == null || !queries.isArray()) {
                throw new InvalidReadSchemaException("table '" + tableName + "' is missing 'queries'");
            }

            queries.forEach(query -> validateQuery(tableName, query));
        });
    }

    private void validateQuery(String tableName, JsonNode query) {
        final JsonNode index = query.get("index");
        if (index == null || !index.isTextual() || index.asText().isBlank()) {
            throw new InvalidReadSchemaException("query in table '" + tableName + "' is missing 'index'");
        }

        final JsonNode key = query.get("key");
        if (key == null || !key.isArray() || key.isEmpty()) {
            throw new InvalidReadSchemaException("query '" + index.asText() + "' in table '" + tableName + "' is missing or has empty 'key'");
        }

        key.forEach(attr -> {
            if (!attr.isTextual() || attr.asText().isBlank()) {
                throw new InvalidReadSchemaException("query '" + index.asText() + "' in table '" + tableName + "' has invalid key attribute");
            }
        });
    }
}