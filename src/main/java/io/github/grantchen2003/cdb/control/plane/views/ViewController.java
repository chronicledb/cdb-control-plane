package io.github.grantchen2003.cdb.control.plane.views;

import io.github.grantchen2003.cdb.control.plane.associations.ForbiddenAssociationException;
import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/views")
public class ViewController {

    private final UserService userService;
    private final ViewService viewService;

    public ViewController(UserService userService, ViewService viewService) {
        this.userService = userService;
        this.viewService = viewService;
    }

    @GetMapping("/{viewId}/replicas")
    public ResponseEntity<?> getViewReplicaEndpoints(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @PathVariable String viewId) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            final List<String> endpoints = viewService.getRunningReplicaEndpoints(userId, viewId);
            return ResponseEntity.ok(Map.of("endpoints", endpoints));
        } catch (ForbiddenAssociationException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    @PostMapping
    public ResponseEntity<?> createView(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @RequestBody @Valid CreateViewRequest request) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            final View view = viewService.createView(userId, request.chronicleName(), request.viewName(), request.readSchemaJson());
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "viewId",        view.viewId(),
                    "userId",        view.userId(),
                    "chronicleName", view.chronicleName(),
                    "viewName",      view.viewName(),
                    "readSchemaId",  view.readSchemaId(),
                    "createdAt",     view.createdAt().toString()
            ));
        } catch (ChronicleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (DuplicateViewException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    public record CreateViewRequest(@NotNull String chronicleName, @NotNull String viewName, @NotNull String readSchemaJson) {}
}