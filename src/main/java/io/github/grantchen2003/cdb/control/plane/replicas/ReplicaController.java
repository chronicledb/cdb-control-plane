package io.github.grantchen2003.cdb.control.plane.replicas;

import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/replicas")
public class ReplicaController {

    private final ChronicleService chronicleService;
    private final ReplicaService replicaService;
    private final UserService userService;

    public ReplicaController(ChronicleService chronicleService, ReplicaService replicaService, UserService userService) {
        this.chronicleService = chronicleService;
        this.replicaService = replicaService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createReplica(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @RequestBody CreateReplicaRequest request) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final boolean chronicleExists = chronicleService.existsByUserIdAndName(userId, request.chronicleName);
        if (!chronicleExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Chronicle not found"));
        }

        final ReplicaType replicaType;
        try {
            replicaType = ReplicaType.valueOf(request.replicaType().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Invalid replica type: " + request.replicaType()));
        }

        final Replica replica = replicaService.createReplica(userId, request.chronicleName, replicaType);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id",               replica.id(),
                "userId",           replica.userId(),
                "chronicleName",    replica.chronicleName(),
                "type",             replica.type(),
                "createdAt",        replica.createdAt().toString()
        ));
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

        replicaService.delete(replica.get());

        return ResponseEntity.noContent().build();
    }

    public record CreateReplicaRequest(String chronicleName, String replicaType) {}
}
