package io.github.grantchen2003.cdb.control.plane.associations;

import io.github.grantchen2003.cdb.control.plane.replicas.ReplicaNotFoundException;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import io.github.grantchen2003.cdb.control.plane.views.ViewNotFoundException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/associations")
public class AssociationController {

    private final AssociationService associationService;
    private final UserService userService;

    public AssociationController(AssociationService associationService, UserService userService) {
        this.associationService = associationService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createAssociation(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @RequestBody @Valid CreateAssociationRequest request) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            final Association association = associationService.createAssociation(userId, request.replicaId(), request.viewId());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "replicaId", association.replicaId(),
                    "viewId",    association.viewId()
            ));
        } catch (ViewNotFoundException | ReplicaNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (ForbiddenAssociationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (AssociationChroniclesMismatchException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
        } catch (DuplicateAssociationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteAssociation(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @RequestBody @Valid DeleteAssociationRequest request) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            associationService.deleteAssociation(userId, request.replicaId(), request.viewId());
            return ResponseEntity.noContent().build();
        } catch (ViewNotFoundException | ReplicaNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (ForbiddenAssociationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (AssociationChroniclesMismatchException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", e.getMessage()));
        }
    }

    public record CreateAssociationRequest(@NotNull String viewId, @NotNull String replicaId) {}
    public record DeleteAssociationRequest(@NotNull String viewId, @NotNull String replicaId) {}
}