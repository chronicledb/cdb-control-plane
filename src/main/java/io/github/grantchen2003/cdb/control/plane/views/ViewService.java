package io.github.grantchen2003.cdb.control.plane.views;

import io.github.grantchen2003.cdb.control.plane.associations.Association;
import io.github.grantchen2003.cdb.control.plane.associations.AssociationService;
import io.github.grantchen2003.cdb.control.plane.associations.ForbiddenAssociationException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ViewService {

    private final AssociationService associationService;
    private final ChronicleService chronicleService;
    private final ReplicaService replicaService;
    private final ViewRepository viewRepository;

    public ViewService(AssociationService associationService, ChronicleService chronicleService, ReplicaService replicaService, ViewRepository viewRepository) {
        this.associationService = associationService;
        this.chronicleService = chronicleService;
        this.replicaService = replicaService;
        this.viewRepository = viewRepository;
    }

    public View createView(String userId, String chronicleName, String viewName) {
        if (!chronicleService.existsByUserIdAndName(userId, chronicleName)) {
            throw new ChronicleNotFoundException();
        }

        if (viewRepository.exists(userId, chronicleName, viewName)) {
            throw new DuplicateViewException();
        }

        final View view = new View(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                viewName,
                Instant.now()
        );

        viewRepository.save(view);

        return view;
    }

    public Optional<View> findByViewId(String viewId) {
        return viewRepository.findByViewId(viewId);
    }

    public boolean exists(String userId, String chronicleName, String viewName) {
        return viewRepository.exists(userId, chronicleName, viewName);
    }

    public List<String> getRunningReplicaEndpoints(String userId, String viewId) {
        final View view = findByViewId(viewId).orElseThrow(ViewNotFoundException::new);
        if (!userId.equals(view.userId())) {
            throw new ForbiddenAssociationException();
        }

        final List<Association> associations = associationService.findByViewId(viewId);
        final List<String> replicaIds = associations.stream().map(Association::replicaId).toList();
        return replicaService.getRunningReplicaEndpoints(replicaIds);
    }
}