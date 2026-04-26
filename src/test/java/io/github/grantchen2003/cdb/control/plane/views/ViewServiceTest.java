package io.github.grantchen2003.cdb.control.plane.views;

import io.github.grantchen2003.cdb.control.plane.associations.Association;
import io.github.grantchen2003.cdb.control.plane.associations.AssociationService;
import io.github.grantchen2003.cdb.control.plane.associations.ForbiddenAssociationException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import io.github.grantchen2003.cdb.control.plane.readschemas.ReadSchema;
import io.github.grantchen2003.cdb.control.plane.readschemas.ReadSchemaService;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewServiceTest {

    private static final String VIEW_ID        = "view-123";
    private static final String USER_ID        = "user-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String VIEW_NAME      = "my-view";
    private static final String READ_SCHEMA_ID = "read-schema-123";
    private static final String READ_SCHEMA_JSON = "{}";
    private static final View   VIEW           = new View(
            VIEW_ID,
            USER_ID,
            CHRONICLE_NAME,
            VIEW_NAME,
            READ_SCHEMA_ID,
            Instant.parse("2024-01-01T00:00:00Z")
    );

    @Mock
    private ViewRepository viewRepository;

    @Mock
    private ChronicleService chronicleService;

    @Mock
    private ReadSchemaService readSchemaService;

    @InjectMocks
    private ViewService viewService;

    @Mock
    private AssociationService associationService;

    @Mock
    private ReplicaService replicaService;

    @Test
    void createView_savesViewAndReturnsIt() {
        when(readSchemaService.createReadSchema(USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON)).thenReturn(new ReadSchema(
                READ_SCHEMA_ID, USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON, Instant.now()
        ));
        when(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(true);

        final ArgumentCaptor<View> captor = ArgumentCaptor.forClass(View.class);

        final View result = viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON);

        verify(viewRepository).save(captor.capture());
        final View saved = captor.getValue();

        assertThat(saved.id()).isNotNull();
        assertThat(saved.userId()).isEqualTo(USER_ID);
        assertThat(saved.chronicleName()).isEqualTo(CHRONICLE_NAME);
        assertThat(saved.viewName()).isEqualTo(VIEW_NAME);
        assertThat(saved.readSchemaId()).isEqualTo(READ_SCHEMA_ID);
        assertThat(saved.createdAt()).isNotNull();
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void exists_viewExists_returnsTrue() {
        when(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(true);

        assertThat(viewService.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).isTrue();
    }

    @Test
    void exists_viewDoesNotExist_returnsFalse() {
        when(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(false);

        assertThat(viewService.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).isFalse();
    }

    @Test
    void findByViewId_viewFound_returnsView() {
        when(viewRepository.findById(VIEW_ID)).thenReturn(Optional.of(VIEW));

        assertThat(viewService.findById(VIEW_ID)).contains(VIEW);
    }

    @Test
    void findByViewId_viewNotFound_returnsEmpty() {
        when(viewRepository.findById(VIEW_ID)).thenReturn(Optional.empty());

        assertThat(viewService.findById(VIEW_ID)).isEmpty();
    }

    @Test
    void createView_chronicleNotFound_throwsChronicleNotFoundException() {
        when(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(false);

        assertThatThrownBy(() -> viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON))
                .isInstanceOf(ChronicleNotFoundException.class);
    }

    @Test
    void createView_duplicateView_throwsDuplicateViewException() {
        when(chronicleService.existsByUserIdAndName(USER_ID, CHRONICLE_NAME)).thenReturn(true);
        when(viewRepository.exists(USER_ID, CHRONICLE_NAME, VIEW_NAME)).thenReturn(true);

        assertThatThrownBy(() -> viewService.createView(USER_ID, CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_JSON))
                .isInstanceOf(DuplicateViewException.class);
    }

    // Add these tests
    @Test
    void getRunningReplicaEndpoints_returnsEndpoints() {
        final List<Association> associations = List.of(new Association("replica-123", VIEW_ID));
        when(viewRepository.findById(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(associationService.findByViewId(VIEW_ID)).thenReturn(associations);
        when(replicaService.getRunningReplicaEndpoints(List.of("replica-123"))).thenReturn(List.of("203.0.113.10:5432"));

        final List<String> endpoints = viewService.getRunningReplicaEndpoints(USER_ID, VIEW_ID);

        assertThat(endpoints).containsExactly("203.0.113.10:5432");
    }

    @Test
    void getRunningReplicaEndpoints_viewNotFound_throwsViewNotFoundException() {
        when(viewRepository.findById(VIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> viewService.getRunningReplicaEndpoints(USER_ID, VIEW_ID))
                .isInstanceOf(ViewNotFoundException.class);
    }

    @Test
    void getRunningReplicaEndpoints_viewOwnedByOtherUser_throwsForbiddenAssociationException() {
        final View otherUsersView = new View(
                VIEW_ID,
                "other-user",
                CHRONICLE_NAME,
                VIEW_NAME,
                READ_SCHEMA_JSON,
                Instant.parse("2024-01-01T00:00:00Z")
        );

        when(viewRepository.findById(VIEW_ID)).thenReturn(Optional.of(otherUsersView));

        assertThatThrownBy(() -> viewService.getRunningReplicaEndpoints(USER_ID, VIEW_ID))
                .isInstanceOf(ForbiddenAssociationException.class);
    }

    @Test
    void getRunningReplicaEndpoints_noAssociations_returnsEmptyList() {
        when(viewRepository.findById(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(associationService.findByViewId(VIEW_ID)).thenReturn(List.of());
        when(replicaService.getRunningReplicaEndpoints(List.of())).thenReturn(List.of());

        assertThat(viewService.getRunningReplicaEndpoints(USER_ID, VIEW_ID)).isEmpty();
    }
}