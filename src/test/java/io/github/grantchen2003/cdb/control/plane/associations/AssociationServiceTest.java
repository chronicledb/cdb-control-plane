package io.github.grantchen2003.cdb.control.plane.associations;

import io.github.grantchen2003.cdb.control.plane.replicas.Replica;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaNotFoundException;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaService;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaStatus;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaType;
import io.github.grantchen2003.cdb.control.plane.views.View;
import io.github.grantchen2003.cdb.control.plane.views.ViewNotFoundException;
import io.github.grantchen2003.cdb.control.plane.views.ViewService;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssociationServiceTest {

    private static final String USER_ID        = "user-123";
    private static final String REPLICA_ID     = "replica-123";
    private static final String VIEW_ID        = "view-123";
    private static final String CHRONICLE_ID   = "chronicle-123";
    private static final String CHRONICLE_NAME = "my-chronicle";
    private static final String VIEW_NAME      = "my-view";
    private static final String READ_SCHEMA_ID = "read-schema-123";
    private static final View VIEW = new View(
            VIEW_ID,
            USER_ID,
            CHRONICLE_NAME,
            VIEW_NAME,
            READ_SCHEMA_ID,
            Instant.parse("2024-01-01T00:00:00Z")
    );
    private static final Replica REPLICA = new Replica(
            REPLICA_ID, USER_ID, CHRONICLE_ID, CHRONICLE_NAME, ReplicaType.REDIS,
            "i-applier-123", "i-storage-123", "i-txmanager-123",
            "203.0.113.10", ReplicaStatus.PROVISIONING, Instant.parse("2024-01-01T00:00:00Z")
    );

    @Mock
    private AssociationRepository associationRepository;

    @Mock
    private ReplicaService replicaService;

    @Mock
    private ViewService viewService;

    @InjectMocks
    private AssociationService associationService;

    @Test
    void createAssociation_savesAndReturnsAssociation() {
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(REPLICA));
        final ArgumentCaptor<Association> captor = ArgumentCaptor.forClass(Association.class);

        final Association result = associationService.createAssociation(USER_ID, REPLICA_ID, VIEW_ID);

        verify(associationRepository).save(captor.capture());
        final Association saved = captor.getValue();
        assertThat(saved.replicaId()).isEqualTo(REPLICA_ID);
        assertThat(saved.viewId()).isEqualTo(VIEW_ID);
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createAssociation_viewNotFound_throwsViewNotFoundException() {
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> associationService.createAssociation(USER_ID, REPLICA_ID, VIEW_ID))
                .isInstanceOf(ViewNotFoundException.class);
    }

    @Test
    void createAssociation_viewOwnedByOtherUser_throwsForbiddenAssociationException() {
        final View otherUsersView = new View(VIEW_ID, "other-user", CHRONICLE_NAME, VIEW_NAME, READ_SCHEMA_ID, Instant.parse("2024-01-01T00:00:00Z"));
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(otherUsersView));

        assertThatThrownBy(() -> associationService.createAssociation(USER_ID, REPLICA_ID, VIEW_ID))
                .isInstanceOf(ForbiddenAssociationException.class);
    }

    @Test
    void createAssociation_replicaNotFound_throwsReplicaNotFoundException() {
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> associationService.createAssociation(USER_ID, REPLICA_ID, VIEW_ID))
                .isInstanceOf(ReplicaNotFoundException.class);
    }

    @Test
    void createAssociation_replicaOwnedByOtherUser_throwsForbiddenAssociationException() {
        final Replica otherUsersReplica = new Replica(
                REPLICA_ID, "other-user", CHRONICLE_ID, CHRONICLE_NAME, ReplicaType.REDIS,
                "i-applier-123", "i-storage-123", "i-txmanager-123",
                "203.0.113.10", ReplicaStatus.PROVISIONING, Instant.parse("2024-01-01T00:00:00Z")
        );
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(otherUsersReplica));

        assertThatThrownBy(() -> associationService.createAssociation(USER_ID, REPLICA_ID, VIEW_ID))
                .isInstanceOf(ForbiddenAssociationException.class);
    }

    @Test
    void createAssociation_chronicleMismatch_throwsAssociationChroniclesMisMatchException() {
        final Replica replicaInOtherChronicle = new Replica(
                REPLICA_ID, USER_ID, CHRONICLE_ID, "other-chronicle", ReplicaType.REDIS,
                "i-applier-123", "i-storage-123", "i-txmanager-123",
                "203.0.113.10", ReplicaStatus.PROVISIONING, Instant.parse("2024-01-01T00:00:00Z")
        );
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(replicaInOtherChronicle));

        assertThatThrownBy(() -> associationService.createAssociation(USER_ID, REPLICA_ID, VIEW_ID))
                .isInstanceOf(AssociationChroniclesMismatchException.class);
    }

    @Test
    void createAssociation_duplicateAssociation_throwsDuplicateAssociationException() {
        when(viewService.findByViewId(VIEW_ID)).thenReturn(Optional.of(VIEW));
        when(replicaService.findById(REPLICA_ID)).thenReturn(Optional.of(REPLICA));
        doThrow(new DuplicateAssociationException(REPLICA_ID, VIEW_ID))
                .when(associationRepository).save(any(Association.class));

        assertThatThrownBy(() -> associationService.createAssociation(USER_ID, REPLICA_ID, VIEW_ID))
                .isInstanceOf(DuplicateAssociationException.class);
    }

    @Test
    void findByViewId_returnsAssociationsFromRepository() {
        final List<Association> expected = List.of(new Association(REPLICA_ID, VIEW_ID));
        when(associationRepository.findByViewId(VIEW_ID)).thenReturn(expected);

        final List<Association> result = associationService.findByViewId(VIEW_ID);

        assertThat(result).isEqualTo(expected);
        verify(associationRepository).findByViewId(VIEW_ID);
    }

    @Test
    void findByViewId_noAssociations_returnsEmptyList() {
        when(associationRepository.findByViewId(VIEW_ID)).thenReturn(List.of());

        assertThat(associationService.findByViewId(VIEW_ID)).isEmpty();
    }
}