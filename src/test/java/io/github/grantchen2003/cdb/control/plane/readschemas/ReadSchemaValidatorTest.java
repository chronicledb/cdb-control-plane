package io.github.grantchen2003.cdb.control.plane.readschemas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReadSchemaValidatorTest {

    private ReadSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReadSchemaValidator();
    }

    @Nested
    class TopLevelStructure {

        @Test
        void validate_invalidJson_throwsException() {
            assertThrows(InvalidReadSchemaException.class, () -> validator.validate("not-json"));
        }

        @Test
        void validate_transactionalRoleMissingTables_throwsException() {
            final String json = "{\"version\": \"2016-09-01\", \"role\": \"TRANSACTIONAL\"}";
            final InvalidReadSchemaException ex = assertThrows(InvalidReadSchemaException.class,
                    () -> validator.validate(json));
            assertTrue(ex.getMessage().contains("tables"));
        }

        @Test
        void validate_validMinimalTransactionalSchema_doesNotThrow() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {}}";
            assertDoesNotThrow(() -> validator.validate(json));
        }
    }

    @Nested
    class TablesValidation {

        @Test
        void validate_tableMissingQueries_throwsException() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {\"Orders\": {}}}";
            final InvalidReadSchemaException ex = assertThrows(InvalidReadSchemaException.class,
                    () -> validator.validate(json));
            assertTrue(ex.getMessage().contains("Orders"));
            assertTrue(ex.getMessage().contains("queries"));
        }

        @Test
        void validate_tableQueriesNotArray_throwsException() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {\"Orders\": {\"queries\": {}}}}";
            assertThrows(InvalidReadSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_tableWithEmptyQueriesArray_doesNotThrow() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {\"Orders\": {\"queries\": []}}}";
            assertDoesNotThrow(() -> validator.validate(json));
        }
    }

    @Nested
    class QueryValidation {

        @Test
        void validate_queryMissingIndex_throwsException() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {\"Orders\": {\"queries\": [{\"key\": [\"CustomerId\"], \"isolation\": \"SERIALIZABLE\"}]}}}";
            final InvalidReadSchemaException ex = assertThrows(InvalidReadSchemaException.class,
                    () -> validator.validate(json));
            assertTrue(ex.getMessage().contains("index"));
        }

        @Test
        void validate_queryMissingKey_throwsException() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {\"Orders\": {\"queries\": [{\"index\": \"customerIdIndex\", \"isolation\": \"SERIALIZABLE\"}]}}}";
            final InvalidReadSchemaException ex = assertThrows(InvalidReadSchemaException.class,
                    () -> validator.validate(json));
            assertTrue(ex.getMessage().contains("key"));
        }

        @Test
        void validate_queryEmptyKey_throwsException() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {\"Orders\": {\"queries\": [{\"index\": \"customerIdIndex\", \"key\": [], \"isolation\": \"SERIALIZABLE\"}]}}}";
            assertThrows(InvalidReadSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_validQueryWithCompositeKey_doesNotThrow() {
            final String json = "{\"version\": \"2016-09-01\", \"tables\": {\"Orders\": {\"queries\": [{\"index\": \"customerIdStatusIndex\", \"key\": [\"CustomerId\", \"Status\"], \"isolation\": \"WEAK_COMMITTED\"}]}}}";
            assertDoesNotThrow(() -> validator.validate(json));
        }
    }
}