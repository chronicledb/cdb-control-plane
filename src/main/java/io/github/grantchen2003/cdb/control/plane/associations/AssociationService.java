package io.github.grantchen2003.cdb.control.plane.associations;

import io.github.grantchen2003.cdb.control.plane.replicas.Replica;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaNotFoundException;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaService;
import io.github.grantchen2003.cdb.control.plane.views.View;
import io.github.grantchen2003.cdb.control.plane.views.ViewNotFoundException;
import io.github.grantchen2003.cdb.control.plane.views.ViewService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AssociationService {

    private final AssociationRepository associationRepository;
    private final ReplicaService replicaService;
    private final ViewService viewService;

    public AssociationService(
            AssociationRepository associationRepository,
            ReplicaService replicaService,
            @Lazy ViewService viewService
    ) {
        this.associationRepository = associationRepository;
        this.replicaService = replicaService;
        this.viewService = viewService;
    }

    public Association createAssociation(String userId, String replicaId, String viewId) {
        final View view = viewService.findById(viewId)
                .orElseThrow(ViewNotFoundException::new);

        if (!userId.equals(view.userId())) {
            throw new ForbiddenAssociationException();
        }

        final Replica replica = replicaService.findById(replicaId)
                .orElseThrow(ReplicaNotFoundException::new);

        if (!userId.equals(replica.userId())) {
            throw new ForbiddenAssociationException();
        }

        if (!replica.chronicleName().equals(view.chronicleName())) {
            throw new AssociationChroniclesMismatchException();
        }

        final Association association = new Association(replicaId, viewId);
        associationRepository.save(association);
        return association;
    }

    public void deleteAssociation(String userId, String replicaId, String viewId) {
        final View view = viewService.findById(viewId).orElseThrow(ViewNotFoundException::new);

        if (!userId.equals(view.userId())) {
            throw new ForbiddenAssociationException();
        }

        final Replica replica = replicaService.findById(replicaId).orElseThrow(ReplicaNotFoundException::new);

        if (!userId.equals(replica.userId())) {
            throw new ForbiddenAssociationException();
        }

        if (!replica.chronicleName().equals(view.chronicleName())) {
            throw new AssociationChroniclesMismatchException();
        }

        associationRepository.delete(viewId, replicaId);
    }

    public List<Association> findByViewId(String viewId) {
        return associationRepository.findByViewId(viewId);
    }

    public List<Association> findByReplicaId(String replicaId) {
        return associationRepository.findByReplicaId(replicaId);
    }
}