package io.github.grantchen2003.cdb.control.plane.readschemas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReadSchemaServiceTest {

    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String VIEW_NAME      = "my-view";

    @Mock
    private ReadSchemaRepository readSchemaRepository;

    @Mock
    private ReadSchemaValidator readSchemaValidator;

    private ReadSchemaService readSchemaService;

    @BeforeEach
    void setUp() {
        readSchemaService = new ReadSchemaService(readSchemaRepository, readSchemaValidator);
    }

    @Test
    void createReadSchema_validSchema_savesToRepositoryAndReturnsObject() {
        final String json = "{\"fields\": [\"id\", \"name\"]}";
        final ArgumentCaptor<ReadSchema> captor = ArgumentCaptor.forClass(ReadSchema.class);

        final ReadSchema result = readSchemaService.createReadSchema(USER_ID, CHRONICLE_NAME, VIEW_NAME, json);

        verify(readSchemaRepository).save(captor.capture());
        final ReadSchema saved = captor.getValue();

        assertThat(saved.id()).isNotNull();
        assertThat(saved.userId()).isEqualTo(USER_ID);
        assertThat(saved.chronicleName()).isEqualTo(CHRONICLE_NAME);
        assertThat(saved.viewName()).isEqualTo(VIEW_NAME);
        assertThat(saved.readSchemaJson()).isEqualTo(json);
        assertThat(saved.createdAt()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createReadSchema_invalidSchema_throwsInvalidReadSchemaException() {
        doThrow(new InvalidReadSchemaException("must be valid JSON"))
                .when(readSchemaValidator).validate(any());

        assertThrows(InvalidReadSchemaException.class,
                () -> readSchemaService.createReadSchema(USER_ID, CHRONICLE_NAME, VIEW_NAME, "not-json"));
    }

    @Test
    void createReadSchema_invalidSchema_doesNotSaveToRepository() {
        doThrow(new InvalidReadSchemaException("must be valid JSON"))
                .when(readSchemaValidator).validate(any());

        assertThrows(InvalidReadSchemaException.class,
                () -> readSchemaService.createReadSchema(USER_ID, CHRONICLE_NAME, VIEW_NAME, "not-json"));

        verify(readSchemaRepository, never()).save(any());
    }
}