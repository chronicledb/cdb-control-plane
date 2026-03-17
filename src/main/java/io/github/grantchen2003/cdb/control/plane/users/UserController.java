package io.github.grantchen2003.cdb.control.plane.users;

import io.github.grantchen2003.cdb.control.plane.utils.ApiKeyUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<?> createUser() {
        final String rawApiKey = ApiKeyUtils.generate();
        userService.createUser(ApiKeyUtils.hash(rawApiKey));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("rawApiKey", rawApiKey));
    }
}