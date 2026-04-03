package io.github.grantchen2003.cdb.control.plane.associations;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AssociationServiceTest {

    private static final String REPLICA_ID = "replica-123";
    private static final String VIEW_ID = "view-123";

    @Mock
    private AssociationRepository associationRepository;

    @InjectMocks
    private AssociationService associationService;

    @Test
    void createAssociation_savesAndReturnsAssociation() {
        final ArgumentCaptor<Association> captor = ArgumentCaptor.forClass(Association.class);

        final Association result = associationService.createAssociation(REPLICA_ID, VIEW_ID);

        verify(associationRepository).save(captor.capture());
        final Association saved = captor.getValue();

        assertThat(saved.replicaId()).isEqualTo(REPLICA_ID);
        assertThat(saved.viewId()).isEqualTo(VIEW_ID);
        assertThat(result).isEqualTo(saved);
    }

    @Test
    void createAssociation_duplicateAssociation_throwsDuplicateAssociationException() {
        doThrow(new DuplicateAssociationException(REPLICA_ID, VIEW_ID))
                .when(associationRepository).save(any(Association.class));

        assertThatThrownBy(() -> associationService.createAssociation(REPLICA_ID, VIEW_ID))
                .isInstanceOf(DuplicateAssociationException.class);
    }
}