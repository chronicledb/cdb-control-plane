package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumValidatorTest {

    private EnumValidator enumValidator;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        enumValidator = new EnumValidator();
    }

    // -------------------------------------------------------------------------
    // Null / non-array inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_nullValues_throwsException() {
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(null)
        );
        assertTrue(ex.getMessage().contains("must be a JSON array"));
    }

    @Test
    void validate_nonArrayNode_throwsException() throws Exception {
        JsonNode objectNode = mapper.readTree("{\"key\": \"value\"}");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(objectNode)
        );
        assertTrue(ex.getMessage().contains("must be a JSON array"));
    }

    @Test
    void validate_textualNode_throwsException() throws Exception {
        JsonNode textNode = mapper.readTree("\"notAnArray\"");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(textNode)
        );
        assertTrue(ex.getMessage().contains("must be a JSON array"));
    }

    // -------------------------------------------------------------------------
    // Empty array
    // -------------------------------------------------------------------------

    @Test
    void validate_emptyArray_throwsException() {
        ArrayNode emptyArray = mapper.createArrayNode();
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(emptyArray)
        );
        assertTrue(ex.getMessage().contains("cannot be empty"));
    }

    // -------------------------------------------------------------------------
    // Invalid element types
    // -------------------------------------------------------------------------

    @Test
    void validate_arrayContainsBoolean_throwsException() throws Exception {
        JsonNode values = mapper.readTree("[\"valid\", true]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(values)
        );
        assertTrue(ex.getMessage().contains("index 1"));
        assertTrue(ex.getMessage().contains("string or a number"));
    }

    @Test
    void validate_arrayContainsNull_throwsException() throws Exception {
        JsonNode values = mapper.readTree("[\"valid\", null]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(values)
        );
        assertTrue(ex.getMessage().contains("index 1"));
        assertTrue(ex.getMessage().contains("string or a number"));
    }

    @Test
    void validate_arrayContainsObject_throwsException() throws Exception {
        JsonNode values = mapper.readTree("[\"valid\", {\"nested\": 1}]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(values)
        );
        assertTrue(ex.getMessage().contains("index 1"));
    }

    @Test
    void validate_arrayContainsNestedArray_throwsException() throws Exception {
        JsonNode values = mapper.readTree("[[1, 2], \"ok\"]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(values)
        );
        assertTrue(ex.getMessage().contains("index 0"));
    }

    @Test
    void validate_firstElementInvalid_reportsIndexZero() throws Exception {
        JsonNode values = mapper.readTree("[true, \"ok\"]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> enumValidator.validate(values)
        );
        assertTrue(ex.getMessage().contains("index 0"));
    }

    // -------------------------------------------------------------------------
    // Valid inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_arrayOfStrings_doesNotThrow() throws Exception {
        JsonNode values = mapper.readTree("[\"red\", \"green\", \"blue\"]");
        assertDoesNotThrow(() -> enumValidator.validate(values));
    }

    @Test
    void validate_arrayOfIntegers_doesNotThrow() throws Exception {
        JsonNode values = mapper.readTree("[1, 2, 3]");
        assertDoesNotThrow(() -> enumValidator.validate(values));
    }

    @Test
    void validate_arrayOfDecimals_doesNotThrow() throws Exception {
        JsonNode values = mapper.readTree("[1.1, 2.2, 3.3]");
        assertDoesNotThrow(() -> enumValidator.validate(values));
    }

    @Test
    void validate_mixedStringsAndNumbers_doesNotThrow() throws Exception {
        JsonNode values = mapper.readTree("[\"active\", 0, \"inactive\", 1]");
        assertDoesNotThrow(() -> enumValidator.validate(values));
    }

    @Test
    void validate_singleStringElement_doesNotThrow() throws Exception {
        JsonNode values = mapper.readTree("[\"only\"]");
        assertDoesNotThrow(() -> enumValidator.validate(values));
    }

    @Test
    void validate_singleNumberElement_doesNotThrow() throws Exception {
        JsonNode values = mapper.readTree("[42]");
        assertDoesNotThrow(() -> enumValidator.validate(values));
    }
}