package io.github.grantchen2003.cdb.control.plane.writeschemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WriteSchemaServiceTest {

    private static final String USER_ID = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";

    @Mock
    private WriteSchemaRepository writeSchemaRepository;

    @Spy
    private ObjectMapper objectMapper;

    @InjectMocks
    private WriteSchemaService writeSchemaService;

    // -------------------------------------------------------------------------
    // save
    // -------------------------------------------------------------------------

    @Test
    void save_validSchema_savesToRepositoryAndReturnsWriteSchema() {
        final String json = validSchema();
        final ArgumentCaptor<WriteSchema> captor = ArgumentCaptor.forClass(WriteSchema.class);

        final WriteSchema result = writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json);

        verify(writeSchemaRepository).save(captor.capture());
        final WriteSchema saved = captor.getValue();
        assertThat(saved.id()).isNotNull();
        assertThat(saved.userId()).isEqualTo(USER_ID);
        assertThat(saved.chronicleName()).isEqualTo(CHRONICLE_NAME);
        assertThat(saved.writeSchemaJson()).isEqualTo(json);
        assertThat(saved.createdAt()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void save_invalidSchema_throwsWithoutSaving() {
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, "not json"))
                .isInstanceOf(InvalidWriteSchemaException.class);
    }

    // -------------------------------------------------------------------------
    // validate — JSON structure
    // -------------------------------------------------------------------------

    @Test
    void validate_invalidJson_throws() {
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, "not json"))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("must be valid JSON");
    }

    @Test
    void validate_emptyJson_throws() {
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, "{}"))
                .isInstanceOf(InvalidWriteSchemaException.class);
    }

    // -------------------------------------------------------------------------
    // validate — types
    // -------------------------------------------------------------------------

    @Test
    void validate_missingTypes_throws() {
        final String json = """
                {
                  "tables": {}
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("types");
    }

    @Test
    void validate_typeMissingVariant_throws() {
        final String json = """
                {
                  "types": {
                    "myType": {}
                  },
                  "tables": {}
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("myType")
                .hasMessageContaining("variant");
    }

    @Test
    void validate_typeInvalidVariant_throws() {
        final String json = """
                {
                  "types": {
                    "myType": { "variant": "boolean" }
                  },
                  "tables": {}
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("myType")
                .hasMessageContaining("boolean");
    }

    @Test
    void validate_allValidTypeVariants_pass() {
        for (final String variant : List.of("text", "integer", "decimal", "binary", "enum")) {
            final String json = """
                    {
                      "types": {
                        "myType": { "variant": "%s" }
                      },
                      "tables": {}
                    }
                    """.formatted(variant);
            writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json);
        }
        verify(writeSchemaRepository, org.mockito.Mockito.times(5)).save(any());
    }

    // -------------------------------------------------------------------------
    // validate — tables
    // -------------------------------------------------------------------------

    @Test
    void validate_missingTables_throws() {
        final String json = """
                {
                  "types": {}
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("tables");
    }

    @Test
    void validate_tableMissingAttributeTypes_throws() {
        final String json = """
                {
                  "types": {
                    "myId": { "variant": "integer" }
                  },
                  "tables": {
                    "Items": {
                      "primaryKey": ["id"]
                    }
                  }
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("Items")
                .hasMessageContaining("attributeTypes");
    }

    @Test
    void validate_attributeTypeReferencesUndefinedType_throws() {
        final String json = """
                {
                  "types": {},
                  "tables": {
                    "Items": {
                      "primaryKey": ["id"],
                      "attributeTypes": { "id": "undefinedType" }
                    }
                  }
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("undefinedType");
    }

    @Test
    void validate_tableMissingPrimaryKey_throws() {
        final String json = """
                {
                  "types": {
                    "myId": { "variant": "integer" }
                  },
                  "tables": {
                    "Items": {
                      "attributeTypes": { "id": "myId" }
                    }
                  }
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("Items")
                .hasMessageContaining("primaryKey");
    }

    @Test
    void validate_primaryKeyFieldNotInAttributeTypes_throws() {
        final String json = """
                {
                  "types": {
                    "myId": { "variant": "integer" }
                  },
                  "tables": {
                    "Items": {
                      "primaryKey": ["missingField"],
                      "attributeTypes": { "id": "myId" }
                    }
                  }
                }
                """;
        assertThatThrownBy(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json))
                .isInstanceOf(InvalidWriteSchemaException.class)
                .hasMessageContaining("Items")
                .hasMessageContaining("missingField");
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private String validSchema() {
        return """
                {
                  "types": {
                    "productId": { "variant": "integer", "range": [0, 1000000000] },
                    "productName": { "variant": "text", "charset": "ascii", "size": [0, 32] }
                  },
                  "tables": {
                    "Products": {
                      "primaryKey": ["ProductId"],
                      "attributeTypes": {
                        "ProductId": "productId",
                        "Name": "productName"
                      }
                    }
                  }
                }
                """;
    }
}