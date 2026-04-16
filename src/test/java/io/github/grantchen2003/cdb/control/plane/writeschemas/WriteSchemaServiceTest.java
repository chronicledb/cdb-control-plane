package io.github.grantchen2003.cdb.control.plane.writeschemas;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.writeschemas.validators.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WriteSchemaServiceTest {

    private static final String USER_ID = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";

    @Mock
    private WriteSchemaRepository writeSchemaRepository;

    private WriteSchemaService writeSchemaService;
    private WriteSchemaValidator validator;

    @BeforeEach
    void setUp() {
        // Instantiate real validator and its dependencies for sociable testing
        validator = new WriteSchemaValidator(
                new IntervalValidator(),
                new EnumValidator(),
                new NumberValidator(),
                new StringValidator()
        );
        writeSchemaService = new WriteSchemaService(writeSchemaRepository, validator);
    }

    // =========================================================================
    // Helpers (Matching Reference Style)
    // =========================================================================

    private String schema(String typesJson, String tablesJson) {
        return String.format("{\"types\": %s, \"tables\": %s}", typesJson, tablesJson);
    }

    private String minimalTable(String typeName) {
        return String.format(
                "{\"users\": {\"attributeTypes\": {\"id\": \"%s\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": []}}",
                typeName
        );
    }

    // =========================================================================
    // Service Logic
    // =========================================================================

    @Test
    void createWriteSchema_validSchema_savesToRepositoryAndReturnsObject() {
        String json = schema("{\"t\": {\"variant\": \"text\"}}", minimalTable("t"));
        ArgumentCaptor<WriteSchema> captor = ArgumentCaptor.forClass(WriteSchema.class);

        WriteSchema result = writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json);

        verify(writeSchemaRepository).save(captor.capture());
        WriteSchema saved = captor.getValue();

        assertThat(saved.id()).isNotNull();
        assertThat(saved.userId()).isEqualTo(USER_ID);
        assertThat(saved.chronicleName()).isEqualTo(CHRONICLE_NAME);
        assertThat(saved.writeSchemaJson()).isEqualTo(json);
        assertThat(saved.createdAt()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    // =========================================================================
    // Top-Level Structure Validation
    // =========================================================================

    @Nested
    class TopLevelStructure {

        @Test
        void validate_invalidJson_throwsException() {
            assertThrows(InvalidWriteSchemaException.class, () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, "not-json"));
        }

        @Test
        void validate_missingTypes_throwsException() {
            String json = "{\"tables\": {}}";
            InvalidWriteSchemaException ex = assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json));
            assertTrue(ex.getMessage().contains("types"));
        }

        @Test
        void validate_emptyTypesAndTables_doesNotThrow() {
            String json = "{\"types\": {}, \"tables\": {}}";
            assertDoesNotThrow(() -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json));
        }
    }

    // =========================================================================
    // Types Validation
    // =========================================================================

    @Nested
    class TypesValidation {

        @Test
        void validate_unknownVariant_throwsException() {
            String json = schema("{\"myType\": {\"variant\": \"uuid\"}}", "{}");
            InvalidWriteSchemaException ex = assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json));
            assertTrue(ex.getMessage().contains("uuid"));
        }

        @Test
        void validate_integerInvertedRange_throwsException() {
            String json = schema("{\"i\": {\"variant\": \"integer\", \"range\": [100, 0]}}", "{}");
            assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json));
        }

        @Test
        void validate_decimalMissingPrecision_throwsException() {
            String json = schema("{\"d\": {\"variant\": \"decimal\"}}", "{}");
            assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json));
        }

        @Test
        void validate_enumEmptyValues_throwsException() {
            String json = schema("{\"e\": {\"variant\": \"enum\", \"values\": []}}", "{}");
            assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, json));
        }
    }

    // =========================================================================
    // Tables Validation
    // =========================================================================

    @Nested
    class TablesValidation {

        private final String validTypes = "{\"myText\": {\"variant\": \"text\"}}";

        @Test
        void validate_attributeReferencesUndefinedType_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"ghostType\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": []}}";
            InvalidWriteSchemaException ex = assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, schema(validTypes, tables)));
            assertTrue(ex.getMessage().contains("ghostType"));
        }

        @Test
        void validate_requiredAttributeNotInAttributeTypes_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [\"missingField\"]}}";
            InvalidWriteSchemaException ex = assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, schema(validTypes, tables)));
            assertTrue(ex.getMessage().contains("missingField"));
        }

        @Test
        void validate_emptyQueryFamily_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [], \"queryFamilies\": [[]]}}";
            InvalidWriteSchemaException ex = assertThrows(InvalidWriteSchemaException.class,
                    () -> writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, schema(validTypes, tables)));
            assertTrue(ex.getMessage().contains("empty query family"));
        }
    }
}