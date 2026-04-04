package io.github.grantchen2003.cdb.control.plane.writeschemas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class WriteSchemaService {

    private static final Set<String> VALID_TYPE_VARIANTS = Set.of("text", "integer", "decimal", "binary", "enum");

    private final WriteSchemaRepository writeSchemaRepository;
    private final ObjectMapper objectMapper;

    public WriteSchemaService(WriteSchemaRepository writeSchemaRepository, ObjectMapper objectMapper) {
        this.writeSchemaRepository = writeSchemaRepository;
        this.objectMapper = objectMapper;
    }

    public WriteSchema createWriteSchema(String userId, String chronicleName, String writeSchemaJson) {
        validate(writeSchemaJson);

        final WriteSchema writeSchema = new WriteSchema(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                writeSchemaJson,
                Instant.now()
        );

        writeSchemaRepository.save(writeSchema);

        return writeSchema;
    }

    private void validate(String writeSchemaJson) {
        final JsonNode root;
        try {
            root = objectMapper.readTree(writeSchemaJson);
        } catch (Exception e) {
            throw new InvalidWriteSchemaException("must be valid JSON");
        }

        final JsonNode types = root.get("types");
        if (types == null || !types.isObject()) {
            throw new InvalidWriteSchemaException("missing required field 'types'");
        }

        types.fields().forEachRemaining(entry -> {
            final JsonNode variant = entry.getValue().get("variant");
            if (variant == null || !variant.isTextual()) {
                throw new InvalidWriteSchemaException("type '" + entry.getKey() + "' is missing 'variant'");
            }
            if (!VALID_TYPE_VARIANTS.contains(variant.asText())) {
                throw new InvalidWriteSchemaException("type '" + entry.getKey() + "' has invalid variant '" + variant.asText() + "'");
            }
        });

        final JsonNode tables = root.get("tables");
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
                if (!types.has(referencedType)) {
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
        });
    }
}