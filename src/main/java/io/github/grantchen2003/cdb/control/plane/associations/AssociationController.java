package io.github.grantchen2003.cdb.control.plane.associations;

import io.github.grantchen2003.cdb.control.plane.replicas.Replica;
import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaService;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import io.github.grantchen2003.cdb.control.plane.views.View;
import io.github.grantchen2003.cdb.control.plane.views.ViewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/associations")
public class AssociationController {

    private final AssociationService associationService;
    private final ReplicaService replicaService;
    private final UserService userService;
    private final ViewService viewService;

    public AssociationController(
            AssociationService associationService,
            ReplicaService replicaService,
            UserService userService,
            ViewService viewService
    ) {
        this.associationService = associationService;
        this.replicaService = replicaService;
        this.userService = userService;
        this.viewService = viewService;
    }

    @PostMapping
    public ResponseEntity<?> createAssociation(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @RequestBody CreateAssociationRequest request) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final Optional<View> view = viewService.findByViewId(request.viewId);
        if (view.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "View not found"));
        }

        if (!userId.equals(view.get().userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        final Optional<Replica> replica = replicaService.findById(request.replicaId());
        if (replica.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Replica not found"));
        }

        if (!userId.equals(replica.get().userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!replica.get().chronicleName().equals(view.get().chronicleName())) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(Map.of("error", "Replica and view must belong to the same chronicle"));
        }

        try {
            final Association association = associationService.createAssociation(request.replicaId(), request.viewId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "replicaId", association.replicaId(),
                    "viewId", association.viewId()
            ));
        } catch (DuplicateAssociationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    public record CreateAssociationRequest(String viewId, String replicaId) {}
}
