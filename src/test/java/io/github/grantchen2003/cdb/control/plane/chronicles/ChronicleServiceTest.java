package io.github.grantchen2003.cdb.control.plane.chronicles;

import io.github.grantchen2003.cdb.control.plane.writeschemas.InvalidWriteSchemaException;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchema;
import io.github.grantchen2003.cdb.control.plane.writeschemas.WriteSchemaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChronicleServiceTest {

    private static final String USER_ID         = "user-123";
    private static final String CHRONICLE_NAME  = "my-chronicle";
    private static final String WRITE_SCHEMA_ID = "schema-123";
    private static final String WRITE_SCHEMA_JSON = """
            {
              "types": { "myId": { "variant": "integer" } },
              "tables": { "Items": { "primaryKey": ["id"], "attributeTypes": { "id": "myId" } } }
            }
            """;
    private static final WriteSchema WRITE_SCHEMA = new WriteSchema(WRITE_SCHEMA_ID, USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON, Instant.parse("2024-01-01T00:00:00Z"));
    private static final Chronicle CHRONICLE      = new Chronicle("chronicle-123", USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_ID, Instant.parse("2024-01-01T00:00:00Z"));

    @Mock
    private ChronicleRepository chronicleRepository;

    @Mock
    private WriteSchemaService writeSchemaService;

    @InjectMocks
    private ChronicleService chronicleService;

    @Test
    void createChronicle_success() {
        when(writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON)).thenReturn(WRITE_SCHEMA);

        final Chronicle chronicle = chronicleService.createChronicle(USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON);

        assertThat(chronicle.id()).isNotNull().isNotEmpty();
        assertThat(chronicle.userId()).isEqualTo(USER_ID);
        assertThat(chronicle.name()).isEqualTo(CHRONICLE_NAME);
        assertThat(chronicle.writeSchemaId()).isEqualTo(WRITE_SCHEMA_ID);
        assertThat(chronicle.createdAt()).isNotNull();
        verify(chronicleRepository).save(any(Chronicle.class));
    }

    @Test
    void createChronicle_invalidWriteSchema_throwsWithoutSavingChronicle() {
        doThrow(new InvalidWriteSchemaException("missing required field 'types'"))
                .when(writeSchemaService).createWriteSchema(USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON);

        assertThatThrownBy(() -> chronicleService.createChronicle(USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON))
                .isInstanceOf(InvalidWriteSchemaException.class);

        verify(chronicleRepository, never()).save(any(Chronicle.class));
    }

    @Test
    void createChronicle_propagatesDuplicateChronicleException() {
        when(writeSchemaService.createWriteSchema(USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON)).thenReturn(WRITE_SCHEMA);
        doThrow(new DuplicateChronicleException(USER_ID, CHRONICLE_NAME))
                .when(chronicleRepository).save(any(Chronicle.class));

        assertThatThrownBy(() -> chronicleService.createChronicle(USER_ID, CHRONICLE_NAME, WRITE_SCHEMA_JSON))
                .isInstanceOf(DuplicateChronicleException.class);
    }

    @Test
    void existsByUserIdAndName_returnsTrue_whenRepositoryReturnsTrue() {
        when(chronicleRepository.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(Optional.of(CHRONICLE));

        assertThat(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).isTrue();
    }

    @Test
    void existsByUserIdAndName_returnsFalse_whenRepositoryReturnsFalse() {
        when(chronicleRepository.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(Optional.empty());

        assertThat(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).isFalse();
    }

    @Test
    void findByUserIdAndName_returnsChronicle_whenFound() {
        when(chronicleRepository.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(Optional.of(CHRONICLE));

        assertThat(chronicleService.findByUserIdAndName(USER_ID, CHRONICLE_NAME))
                .isPresent()
                .contains(CHRONICLE);
    }

    @Test
    void findByUserIdAndName_returnsEmpty_whenNotFound() {
        when(chronicleRepository.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(Optional.empty());

        assertThat(chronicleService.findByUserIdAndName(USER_ID, CHRONICLE_NAME)).isEmpty();
    }
}