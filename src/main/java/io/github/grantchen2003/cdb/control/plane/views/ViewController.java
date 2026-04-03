package io.github.grantchen2003.cdb.control.plane.views;

import io.github.grantchen2003.cdb.control.plane.chronicles.ChronicleService;
import io.github.grantchen2003.cdb.control.plane.users.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/views")
public class ViewController {

    private final ChronicleService chronicleService;
    private final UserService userService;
    private final ViewService viewService;

    public ViewController(ChronicleService chronicleService, UserService userService, ViewService viewService) {
        this.chronicleService = chronicleService;
        this.userService = userService;
        this.viewService = viewService;
    }

    @PostMapping
    public ResponseEntity<?> createReplica(
            @RequestHeader("X-Api-Key") String rawApiKey,
            @RequestBody CreateViewRequest request) {

        final String userId = userService.findUserIdByRawApiKey(rawApiKey).orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final boolean chronicleExists = chronicleService.existsByUserIdAndName(userId, request.chronicleName);
        if (!chronicleExists) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Chronicle not found"));
        }

        final boolean sameNameViewExists = viewService.exists(userId, request.chronicleName(), request.viewName());
        if (sameNameViewExists) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "View already exists"));
        }

        final View view = viewService.createView(userId, request.chronicleName(), request.viewName());

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "userId", view.userId(),
                "chronicleName", view.chronicleName(),
                "viewName", view.viewName()
        ));
    }

    public record CreateViewRequest(String chronicleName, String viewName) {}
}
