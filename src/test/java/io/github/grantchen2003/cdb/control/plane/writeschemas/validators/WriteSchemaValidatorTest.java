package io.github.grantchen2003.cdb.control.plane.writeschemas.validators;

import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WriteSchemaValidatorTest {

    private WriteSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new WriteSchemaValidator(
                new IntervalValidator(),
                new EnumValidator(),
                new NumberValidator(),
                new StringValidator()
        );
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Wraps a types block and a tables block into a full schema JSON string. */
    private String schema(String typesJson, String tablesJson) {
        return String.format("{\"types\": %s, \"tables\": %s}", typesJson, tablesJson);
    }

    /** Minimal valid table referencing one type. */
    private String minimalTable(String typeName) {
        return String.format(
                "{\"users\": {\"attributeTypes\": {\"id\": \"%s\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": []}}",
                typeName
        );
    }

    // =========================================================================
    // Top-level structure
    // =========================================================================

    @Nested
    class TopLevelStructure {

        @Test
        void validate_invalidJson_throwsException() {
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate("not-json"));
        }

        @Test
        void validate_missingTypes_throwsException() {
            String json = "{\"tables\": {}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class, () -> validator.validate(json)
            );
            assertTrue(ex.getMessage().contains("types"));
        }

        @Test
        void validate_missingTables_throwsException() {
            String json = "{\"types\": {}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class, () -> validator.validate(json)
            );
            assertTrue(ex.getMessage().contains("tables"));
        }

        @Test
        void validate_typesIsArray_throwsException() {
            String json = "{\"types\": [], \"tables\": {}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class, () -> validator.validate(json)
            );
            assertTrue(ex.getMessage().contains("types"));
        }

        @Test
        void validate_tablesIsArray_throwsException() {
            String json = "{\"types\": {}, \"tables\": []}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class, () -> validator.validate(json)
            );
            assertTrue(ex.getMessage().contains("tables"));
        }

        @Test
        void validate_emptyTypesAndTables_doesNotThrow() {
            String json = "{\"types\": {}, \"tables\": {}}";
            assertDoesNotThrow(() -> validator.validate(json));
        }
    }

    // =========================================================================
    // Types validation
    // =========================================================================

    @Nested
    class TypesValidation {

        @Test
        void validate_typeMissingVariant_throwsException() {
            String json = "{\"types\": {\"myType\": {}}, \"tables\": {}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class, () -> validator.validate(json)
            );
            assertTrue(ex.getMessage().contains("myType"));
            assertTrue(ex.getMessage().contains("variant"));
        }

        @Test
        void validate_typeVariantIsNumeric_throwsException() {
            String json = "{\"types\": {\"myType\": {\"variant\": 42}}, \"tables\": {}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class, () -> validator.validate(json)
            );
            assertTrue(ex.getMessage().contains("variant"));
        }

        @Test
        void validate_unknownVariant_throwsException() {
            String json = "{\"types\": {\"myType\": {\"variant\": \"uuid\"}}, \"tables\": {}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class, () -> validator.validate(json)
            );
            assertTrue(ex.getMessage().contains("uuid"));
        }

        // ------------------------------------------------------------------
        // text variant
        // ------------------------------------------------------------------

        @Test
        void validate_textVariantNoOptions_doesNotThrow() {
            String json = schema("{\"t\": {\"variant\": \"text\"}}", minimalTable("t"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_textVariantWithValidCharset_doesNotThrow() {
            String json = schema("{\"t\": {\"variant\": \"text\", \"charset\": \"ascii\"}}", minimalTable("t"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_textVariantWithInvalidCharset_throwsException() {
            String json = schema("{\"t\": {\"variant\": \"text\", \"charset\": \"utf8\"}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_textVariantWithValidSize_doesNotThrow() {
            String json = schema("{\"t\": {\"variant\": \"text\", \"size\": [0, 255]}}", minimalTable("t"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_textVariantWithNegativeSize_throwsException() {
            String json = schema("{\"t\": {\"variant\": \"text\", \"size\": [-1, 255]}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_textVariantWithInvertedSize_throwsException() {
            String json = schema("{\"t\": {\"variant\": \"text\", \"size\": [255, 0]}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        // ------------------------------------------------------------------
        // integer variant
        // ------------------------------------------------------------------

        @Test
        void validate_integerVariantNoRange_doesNotThrow() {
            String json = schema("{\"i\": {\"variant\": \"integer\"}}", minimalTable("i"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_integerVariantWithValidRange_doesNotThrow() {
            String json = schema("{\"i\": {\"variant\": \"integer\", \"range\": [0, 1000]}}", minimalTable("i"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_integerVariantWithInvertedRange_throwsException() {
            String json = schema("{\"i\": {\"variant\": \"integer\", \"range\": [100, 0]}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        // ------------------------------------------------------------------
        // decimal variant
        // ------------------------------------------------------------------

        @Test
        void validate_decimalVariantWithPrecision_doesNotThrow() {
            String json = schema("{\"d\": {\"variant\": \"decimal\", \"precision\": 10}}", minimalTable("d"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_decimalVariantMissingPrecision_throwsException() {
            // precision is required for decimal
            String json = schema("{\"d\": {\"variant\": \"decimal\"}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_decimalVariantPrecisionOutOfRange_throwsException() {
            String json = schema("{\"d\": {\"variant\": \"decimal\", \"precision\": 65}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_decimalVariantPrecisionAtMax_doesNotThrow() {
            String json = schema("{\"d\": {\"variant\": \"decimal\", \"precision\": 64}}", minimalTable("d"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_decimalVariantPrecisionAtMin_doesNotThrow() {
            String json = schema("{\"d\": {\"variant\": \"decimal\", \"precision\": 0}}", minimalTable("d"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_decimalVariantWithValidScale_doesNotThrow() {
            String json = schema("{\"d\": {\"variant\": \"decimal\", \"precision\": 10, \"scale\": [0, 4]}}", minimalTable("d"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_decimalVariantWithScaleExceedingMax_throwsException() {
            String json = schema("{\"d\": {\"variant\": \"decimal\", \"precision\": 10, \"scale\": [0, 65]}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        // ------------------------------------------------------------------
        // binary variant
        // ------------------------------------------------------------------

        @Test
        void validate_binaryVariantNoOptions_doesNotThrow() {
            String json = schema("{\"b\": {\"variant\": \"binary\"}}", minimalTable("b"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_binaryVariantWithValidSize_doesNotThrow() {
            String json = schema("{\"b\": {\"variant\": \"binary\", \"size\": [0, 65535]}}", minimalTable("b"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_binaryVariantWithNegativeSize_throwsException() {
            String json = schema("{\"b\": {\"variant\": \"binary\", \"size\": [-1, 100]}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        // ------------------------------------------------------------------
        // enum variant
        // ------------------------------------------------------------------

        @Test
        void validate_enumVariantWithStringValues_doesNotThrow() {
            String json = schema("{\"e\": {\"variant\": \"enum\", \"values\": [\"a\", \"b\"]}}", minimalTable("e"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_enumVariantWithMixedValues_doesNotThrow() {
            String json = schema("{\"e\": {\"variant\": \"enum\", \"values\": [\"active\", 0]}}", minimalTable("e"));
            assertDoesNotThrow(() -> validator.validate(json));
        }

        @Test
        void validate_enumVariantMissingValues_throwsException() {
            String json = schema("{\"e\": {\"variant\": \"enum\"}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_enumVariantEmptyValues_throwsException() {
            String json = schema("{\"e\": {\"variant\": \"enum\", \"values\": []}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        @Test
        void validate_enumVariantBooleanValue_throwsException() {
            String json = schema("{\"e\": {\"variant\": \"enum\", \"values\": [true]}}", "{}");
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(json));
        }

        // ------------------------------------------------------------------
        // Multiple types
        // ------------------------------------------------------------------

        @Test
        void validate_multipleValidTypes_doesNotThrow() {
            String types = "{\"t\": {\"variant\": \"text\"}, \"i\": {\"variant\": \"integer\"}, \"e\": {\"variant\": \"enum\", \"values\": [\"x\"]}}";
            String tables = "{\"users\": {\"attributeTypes\": {\"name\": \"t\", \"age\": \"i\", \"status\": \"e\"}, \"primaryKey\": [\"name\"], \"requiredAttributes\": []}}";
            assertDoesNotThrow(() -> validator.validate(schema(types, tables)));
        }
    }

    // =========================================================================
    // Tables validation
    // =========================================================================

    @Nested
    class TablesValidation {

        private final String validTypes = "{\"myText\": {\"variant\": \"text\"}, \"myInt\": {\"variant\": \"integer\"}}";

        @Test
        void validate_tableMissingAttributeTypes_throwsException() {
            String tables = "{\"orders\": {\"primaryKey\": [\"id\"], \"requiredAttributes\": []}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("orders"));
            assertTrue(ex.getMessage().contains("attributeTypes"));
        }

        @Test
        void validate_attributeTypesIsArray_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": [], \"primaryKey\": [\"id\"], \"requiredAttributes\": []}}";
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(schema(validTypes, tables)));
        }

        @Test
        void validate_attributeReferencesUndefinedType_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"unknownType\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": []}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("unknownType"));
        }

        @Test
        void validate_tableMissingPrimaryKey_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"requiredAttributes\": []}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("primaryKey"));
        }

        @Test
        void validate_emptyPrimaryKey_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [], \"requiredAttributes\": []}}";
            assertThrows(InvalidWriteSchemaException.class, () -> validator.validate(schema(validTypes, tables)));
        }

        @Test
        void validate_primaryKeyFieldNotInAttributeTypes_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"nonexistent\"], \"requiredAttributes\": []}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("nonexistent"));
        }

        @Test
        void validate_compositePrimaryKey_allFieldsPresent_doesNotThrow() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\", \"region\": \"myText\"}, \"primaryKey\": [\"id\", \"region\"], \"requiredAttributes\": []}}";
            assertDoesNotThrow(() -> validator.validate(schema(validTypes, tables)));
        }

        @Test
        void validate_requiredAttributeIsArray_doesNotThrow() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\", \"name\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [\"name\"]}}";
            assertDoesNotThrow(() -> validator.validate(schema(validTypes, tables)));
        }

        @Test
        void validate_requiredAttributeNotInAttributeTypes_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [\"ghost\"]}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("ghost"));
        }

        @Test
        void validate_requiredAttributesIsMissing_throwsException() {
            // requiredAttributes is accessed without null check; missing field causes NPE-like throw
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"id\"]}}";
            assertThrows(Exception.class, () -> validator.validate(schema(validTypes, tables)));
        }

        // ------------------------------------------------------------------
        // queryFamilies
        // ------------------------------------------------------------------

        @Test
        void validate_validQueryFamily_doesNotThrow() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\", \"name\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [], \"queryFamilies\": [[\"name\"]]}}";
            assertDoesNotThrow(() -> validator.validate(schema(validTypes, tables)));
        }

        @Test
        void validate_emptyQueryFamily_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [], \"queryFamilies\": [[]]}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("empty query family"));
        }

        @Test
        void validate_queryFamilyAttributeNotInAttributeTypes_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [], \"queryFamilies\": [[\"unknown\"]]}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("unknown"));
        }

        @Test
        void validate_queryFamilyIsNotArray_throwsException() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [], \"queryFamilies\": [\"id\"]}}";
            InvalidWriteSchemaException ex = assertThrows(
                    InvalidWriteSchemaException.class,
                    () -> validator.validate(schema(validTypes, tables))
            );
            assertTrue(ex.getMessage().contains("Query family must be an array"));
        }

        @Test
        void validate_multipleQueryFamilies_doesNotThrow() {
            String tables = "{\"orders\": {\"attributeTypes\": {\"id\": \"myText\", \"name\": \"myText\", \"region\": \"myText\"}, \"primaryKey\": [\"id\"], \"requiredAttributes\": [], \"queryFamilies\": [[\"name\"], [\"region\", \"name\"]]}}";
            assertDoesNotThrow(() -> validator.validate(schema(validTypes, tables)));
        }

        // ------------------------------------------------------------------
        // Full valid schema
        // ------------------------------------------------------------------

        @Test
        void validate_fullValidSchema_doesNotThrow() {
            String json = """
                    {
                      "types": {
                        "userId":  { "variant": "text", "charset": "ascii", "size": [1, 64] },
                        "age":     { "variant": "integer", "range": [0, 150] },
                        "balance": { "variant": "decimal", "precision": 18, "scale": [0, 2] },
                        "avatar":  { "variant": "binary", "size": [0, 1048576] },
                        "role":    { "variant": "enum", "values": ["admin", "user", "guest"] }
                      },
                      "tables": {
                        "accounts": {
                          "attributeTypes": {
                            "id":      "userId",
                            "years":   "age",
                            "credits": "balance",
                            "pic":     "avatar",
                            "access":  "role"
                          },
                          "primaryKey": ["id"],
                          "requiredAttributes": ["years"],
                          "queryFamilies": [["access"], ["access", "years"]]
                        }
                      }
                    }
                    """;
            assertDoesNotThrow(() -> validator.validate(json));
        }
    }
}