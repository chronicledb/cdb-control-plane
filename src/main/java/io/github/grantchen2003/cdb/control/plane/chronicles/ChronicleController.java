package io.github.grantchen2003.cdb.control.plane.chronicles;

import io.github.grantchen2003.cdb.control.plane.users.UserService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/chronicles")
public class ChronicleController {

    private final ChronicleService chronicleService;
    private final UserService userService;

    public ChronicleController(ChronicleService chronicleService, UserService userService) {
        this.chronicleService = chronicleService;
        this.userService = userService;
    }

    @PostMapping("/{chronicleName}")
    public ResponseEntity<?> createChronicle(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @PathVariable @NotBlank String chronicleName) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            final Chronicle chronicle = chronicleService.createChronicle(userId, chronicleName);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "userId",    chronicle.userId(),
                    "name",      chronicle.name(),
                    "createdAt", chronicle.createdAt().toString()
            ));
        } catch (DuplicateChronicleException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }
}