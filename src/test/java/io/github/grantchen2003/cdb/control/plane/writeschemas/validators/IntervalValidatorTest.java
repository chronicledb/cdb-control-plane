package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntervalValidatorTest {

    private IntervalValidator intervalValidator;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        intervalValidator = new IntervalValidator();
    }

    // -------------------------------------------------------------------------
    // Null / non-array inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_nullInterval_throwsException() {
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(null, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be a JSON array"));
    }

    @Test
    void validate_objectNode_throwsException() throws Exception {
        JsonNode node = mapper.readTree("{\"a\": 1}");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be a JSON array"));
    }

    @Test
    void validate_scalarNumber_throwsException() throws Exception {
        JsonNode node = mapper.readTree("42");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be a JSON array"));
    }

    // -------------------------------------------------------------------------
    // Wrong number of elements
    // -------------------------------------------------------------------------

    @Test
    void validate_emptyArray_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("exactly 2 elements"));
    }

    @Test
    void validate_singleElementArray_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[5]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("exactly 2 elements"));
    }

    @Test
    void validate_threeElementArray_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[1, 2, 3]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("exactly 2 elements"));
    }

    // -------------------------------------------------------------------------
    // Non-numeric elements
    // -------------------------------------------------------------------------

    @Test
    void validate_lowerBoundIsString_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[\"a\", 10]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be numeric"));
    }

    @Test
    void validate_upperBoundIsString_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[0, \"b\"]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be numeric"));
    }

    @Test
    void validate_bothBoundsAreStrings_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[\"a\", \"b\"]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be numeric"));
    }

    @Test
    void validate_elementIsBoolean_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[true, 10]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be numeric"));
    }

    // -------------------------------------------------------------------------
    // Out-of-bounds checks
    // -------------------------------------------------------------------------

    @Test
    void validate_lowerBoundBelowMinimum_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[-1, 50]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("below minimum"));
    }

    @Test
    void validate_upperBoundAboveMaximum_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[0, 101]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("above maximum"));
    }

    @Test
    void validate_bothBoundsOutOfRange_lowerCheckedFirst() throws Exception {
        // Lower bound is checked before upper bound
        JsonNode node = mapper.readTree("[-5, 200]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("below minimum"));
    }

    // -------------------------------------------------------------------------
    // Inverted bounds (lower > upper)
    // -------------------------------------------------------------------------

    @Test
    void validate_lowerBoundGreaterThanUpperBound_throwsException() throws Exception {
        JsonNode node = mapper.readTree("[80, 20]");
        InvalidWriteSchemaException ex = assertThrows(
                InvalidWriteSchemaException.class,
                () -> intervalValidator.validate(node, 0, 100)
        );
        assertTrue(ex.getMessage().contains("must be <="));
    }

    // -------------------------------------------------------------------------
    // Valid inputs
    // -------------------------------------------------------------------------

    @Test
    void validate_normalInterval_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("[10, 90]");
        assertDoesNotThrow(() -> intervalValidator.validate(node, 0, 100));
    }

    @Test
    void validate_equalBounds_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("[50, 50]");
        assertDoesNotThrow(() -> intervalValidator.validate(node, 0, 100));
    }

    @Test
    void validate_boundsExactlyAtLimits_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("[0, 100]");
        assertDoesNotThrow(() -> intervalValidator.validate(node, 0, 100));
    }

    @Test
    void validate_decimalBounds_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("[0.5, 99.5]");
        assertDoesNotThrow(() -> intervalValidator.validate(node, 0, 100));
    }

    @Test
    void validate_negativeRangeInterval_doesNotThrow() throws Exception {
        JsonNode node = mapper.readTree("[-50, -10]");
        assertDoesNotThrow(() -> intervalValidator.validate(node, -100, 0));
    }
}