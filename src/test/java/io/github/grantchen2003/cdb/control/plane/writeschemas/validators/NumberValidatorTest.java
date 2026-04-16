package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberValidatorTest {

    private NumberValidator numberValidator;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        numberValidator = new NumberValidator();
    }

    // -------------------------------------------------------------------------
    // Null / non-numeric inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_nullNode_throwsException() {
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(null, 0, 100)
        );
        assertTrue(ex.getMessage().contains("numeric type"));
    }

    @Test
    void validate_stringNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("\"42\"");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("numeric type"));
    }

    @Test
    void validate_booleanNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("true");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("numeric type"));
    }

    @Test
    void validate_arrayNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[1, 2]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("numeric type"));
    }

    @Test
    void validate_objectNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("{\"value\": 5}");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("numeric type"));
    }

    // -------------------------------------------------------------------------
    // Out-of-bounds checks
    // -------------------------------------------------------------------------

    @Test
    void validate_valueBelowMin_throwsException() throws Exception {
        JsonNode node = mapper.readTree("-1");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("out of bounds"));
        assertTrue(ex.getMessage().contains("-1"));
    }

    @Test
    void validate_valueAboveMax_throwsException() throws Exception {
        JsonNode node = mapper.readTree("101");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("out of bounds"));
        assertTrue(ex.getMessage().contains("101"));
    }

    @Test
    void validate_errorMessageContainsBounds() throws Exception {
        JsonNode node = mapper.readTree("200");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 10, 50)
        );
        String msg = ex.getMessage();
        assertTrue(msg.contains("10"));
        assertTrue(msg.contains("50"));
        assertTrue(msg.contains("200"));
    }

    // -------------------------------------------------------------------------
    // Valid inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_valueAtMin_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("0");
        assertDoesNotThrow(() -> numberValidator.validate(node, 0, 100));
    }

    @Test
    void validate_valueAtMax_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("100");
        assertDoesNotThrow(() -> numberValidator.validate(node, 0, 100));
    }

    @Test
    void validate_valueInMiddle_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("50");
        assertDoesNotThrow(() -> numberValidator.validate(node, 0, 100));
    }

    @Test
    void validate_negativeValueWithinNegativeRange_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("-50");
        assertDoesNotThrow(() -> numberValidator.validate(node, -100, 0));
    }

    @Test
    void validate_minEqualToMax_exactValuePasses() throws Exception {
        JsonNode node = mapper.readTree("42");
        assertDoesNotThrow(() -> numberValidator.validate(node, 42, 42));
    }

    @Test
    void validate_minEqualToMax_wrongValueFails() throws Exception {
        JsonNode node = mapper.readTree("43");
        assertThrows(
                InvalidWriteSchemaException.class,
                () -> numberValidator.validate(node, 42, 42)
        );
    }

    @Test
    void validate_longMinValue_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree(String.valueOf(Long.MIN_VALUE));
        assertDoesNotThrow(() -> numberValidator.validate(node, Long.MIN_VALUE, Long.MAX_VALUE));
    }

    @Test
    void validate_longMaxValue_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree(String.valueOf(Long.MAX_VALUE));
        assertDoesNotThrow(() -> numberValidator.validate(node, Long.MIN_VALUE, Long.MAX_VALUE));
    }
}