package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleNotFoundException;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/replicas")
public class ReplicaController {

    private final ReplicaService replicaService;
    private final UserService userService;

    public ReplicaController(ReplicaService replicaService, UserService userService) {
        this.replicaService = replicaService;
        this.userService = userService;
    }

    @GetMapping("/{replicaId}")
    public ResponseEntity<?> getReplica(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @PathVariable String replicaId) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final Optional<Replica> replicaOpt = replicaService.findById(replicaId);
        if (replicaOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!userId.equals(replicaOpt.get().userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(toResponseBody(replicaOpt.get()));
    }

    @PostMapping
    public ResponseEntity<?> createReplica(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @RequestBody @Valid CreateReplicaRequest request) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            final Replica replica = replicaService.createReplica(userId, request.chronicleName(), request.replicaType());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponseBody(replica));
        } catch (ChronicleNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (InvalidReplicaTypeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{replicaId}")
    public ResponseEntity<?> deleteReplica(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @PathVariable String replicaId) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final Optional<Replica> replica = replicaService.findById(replicaId);
        if (replica.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (!userId.equals(replica.get().userId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            replicaService.delete(replica.get());
            return ResponseEntity.noContent().build();
        } catch (ReplicaInUseException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toResponseBody(Replica replica) {
        final Map<String, Object> body = new HashMap<>();
        body.put("id",            replica.id());
        body.put("userId",        replica.userId());
        body.put("chronicleName", replica.chronicleName());
        body.put("type",          replica.type());
        body.put("status",        replica.status());
        body.put("createdAt",     replica.createdAt().toString());

        if (replica.txManagerPublicIp() != null) {
            body.put("publicIp", replica.txManagerPublicIp());
        }

        return body;
    }

    public record CreateReplicaRequest(@NotNull String chronicleName, @NotNull String replicaType) {}
}