package io.github.grantchen2003.cdb.control.plane.views;

import io.github.grantchen2003.cdb.control.plane.associations.Association;
import io.github.grantchen2003.cdb.control.plane.associations.AssociationService;
import io.github.grantchen2003.cdb.control.plane.associations.ForbiddenAssociationException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import io.github.grantchen2003.cdb.control.plane.readschemas.ReadSchema;
import io.github.grantchen2003.cdb.control.plane.readschemas.ReadSchemaService;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ViewService {

    private static final Logger log = LoggerFactory.getLogger(ViewService.class);

    private final AssociationService associationService;
    private final ChronicleService chronicleService;
    private final ReadSchemaService readSchemaService;
    private final ReplicaService replicaService;
    private final ViewRepository viewRepository;

    public ViewService(
            AssociationService associationService,
            ChronicleService chronicleService,
            ReadSchemaService readSchemaService,
            ReplicaService replicaService,
            ViewRepository viewRepository) {
        this.associationService = associationService;
        this.chronicleService = chronicleService;
        this.readSchemaService = readSchemaService;
        this.replicaService = replicaService;
        this.viewRepository = viewRepository;
    }

    public View createView(String userId, String chronicleName, String viewName, String readSchemaJson) {
        if (!chronicleService.existsByUserIdAndName(userId, chronicleName)) {
            throw new ChronicleNotFoundException();
        }

        if (viewRepository.exists(userId, chronicleName, viewName)) {
            throw new DuplicateViewException();
        }

        final ReadSchema readSchema = readSchemaService.createReadSchema(
                userId, chronicleName, viewName, readSchemaJson);

        final View view = new View(
                UUID.randomUUID().toString(),
                userId,
                chronicleName,
                viewName,
                readSchema.id(),
                Instant.now()
        );

        try {
            viewRepository.save(view);
        } catch (Exception e) {
            log.error("View save failed after read schema save — orphaned read schema for userId: {} chronicleName: {} viewName: {}",
                    userId, chronicleName, viewName);
            throw e;
        }

        return view;
    }

    public Optional<View> findById(String id) {
        return viewRepository.findById(id);
    }

    public boolean exists(String userId, String chronicleName, String viewName) {
        return viewRepository.exists(userId, chronicleName, viewName);
    }

    public List<String> getRunningReplicaEndpoints(String userId, String viewId) {
        final View view = findById(viewId).orElseThrow(ViewNotFoundException::new);
        if (!userId.equals(view.userId())) {
            throw new ForbiddenAssociationException();
        }

        final List<Association> associations = associationService.findByViewId(viewId);
        final List<String> replicaIds = associations.stream().map(Association::replicaId).toList();
        return replicaService.getRunningReplicaEndpoints(replicaIds);
    }
}