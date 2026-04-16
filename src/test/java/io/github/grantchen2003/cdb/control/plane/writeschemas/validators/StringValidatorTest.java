package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringValidatorTest {

    private StringValidator stringValidator;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        stringValidator = new StringValidator();
    }

    // -------------------------------------------------------------------------
    // Null / non-string inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_nullNode_throwsException() {
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(null, List.of("a", "b"))
        );
        assertTrue(ex.getMessage().contains("must be a string"));
    }

    @Test
    void validate_numericNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("42");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of("42"))
        );
        assertTrue(ex.getMessage().contains("must be a string"));
    }

    @Test
    void validate_booleanNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("true");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of("true"))
        );
        assertTrue(ex.getMessage().contains("must be a string"));
    }

    @Test
    void validate_arrayNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[\"a\"]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of("a"))
        );
        assertTrue(ex.getMessage().contains("must be a string"));
    }

    @Test
    void validate_objectNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("{\"key\": \"a\"}");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of("a"))
        );
        assertTrue(ex.getMessage().contains("must be a string"));
    }

    // -------------------------------------------------------------------------
    // Value not in valid list
    // -------------------------------------------------------------------------

    @Test
    void validate_valueNotInList_throwsException() throws Exception {
        JsonNode node = mapper.readTree("\"utf8\"");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of("ascii", "unicode"))
        );
        assertTrue(ex.getMessage().contains("utf8"));
        assertTrue(ex.getMessage().contains("ascii"));
        assertTrue(ex.getMessage().contains("unicode"));
    }

    @Test
    void validate_caseMismatch_throwsException() throws Exception {
        // Validation is case-sensitive
        JsonNode node = mapper.readTree("\"ASCII\"");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of("ascii", "unicode"))
        );
        assertTrue(ex.getMessage().contains("ASCII"));
    }

    @Test
    void validate_emptyStringNotInList_throwsException() throws Exception {
        JsonNode node = mapper.readTree("\"\"");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of("ascii", "unicode"))
        );
        assertTrue(ex.getMessage().contains("Invalid value"));
    }

    // -------------------------------------------------------------------------
    // Empty valid-values list edge case
    // -------------------------------------------------------------------------

    @Test
    void validate_emptyValidList_anyStringThrows() throws Exception {
        JsonNode node = mapper.readTree("\"anything\"");
        assertThrows(
                InvalidWriteSchemaException.class,
                () -> stringValidator.validate(node, List.of())
        );
    }

    // -------------------------------------------------------------------------
    // Valid inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_valueExactlyInList_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("\"ascii\"");
        assertDoesNotThrow(() -> stringValidator.validate(node, List.of("ascii", "unicode")));
    }

    @Test
    void validate_secondValueInList_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("\"unicode\"");
        assertDoesNotThrow(() -> stringValidator.validate(node, List.of("ascii", "unicode")));
    }

    @Test
    void validate_singleElementList_matchingValue_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("\"only\"");
        assertDoesNotThrow(() -> stringValidator.validate(node, List.of("only")));
    }

    @Test
    void validate_valueWithinLargerList_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("\"c\"");
        assertDoesNotThrow(() -> stringValidator.validate(node, List.of("a", "b", "c", "d", "e")));
    }
}