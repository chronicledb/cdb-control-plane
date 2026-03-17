package io.github.grantchen2003.cdb.control.plane.users;

import io.github.grantchen2003.cdb.control.plane.utils.ApiKeyUtils;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String hashedApiKey) {
        final User user = new User(
                UUID.randomUUID().toString(),
                hashedApiKey,
                Instant.now()
        );

        userRepository.save(user);

        return user;
    }

    public boolean verifyApiKey(String userId, String rawApiKey) {
        return userRepository.findById(userId)
                .map(user -> ApiKeyUtils.verify(rawApiKey, user.hashedApiKey()))
                .orElse(false);
    }
}