package io.github.grantchen2003.cdb.control.plane.users;

import io.github.grantchen2003.cdb.control.plane.utils.ApiKeyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createUser_success() {
        final String hashedApiKey = ApiKeyUtils.hash(ApiKeyUtils.generate());
        final User user = userService.createUser(hashedApiKey);

        assertThat(user.id()).isNotNull();
        assertThat(user.hashedApiKey()).isEqualTo(hashedApiKey);
        assertThat(user.createdAt()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    // -----------------------------------------------------------------------
    // verifyApiKey
    // -----------------------------------------------------------------------

    @Test
    void verifyApiKey_returnsTrueForCorrectKey() {
        final String userId = "user-123";
        final String rawApiKey = ApiKeyUtils.generate();
        final User user = new User(userId, ApiKeyUtils.hash(rawApiKey), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(userService.verifyApiKey(userId, rawApiKey)).isTrue();
    }

    @Test
    void verifyApiKey_returnsFalseForWrongKey() {
        final String userId = "user-123";
        final User user = new User(userId, ApiKeyUtils.hash(ApiKeyUtils.generate()), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThat(userService.verifyApiKey(userId, ApiKeyUtils.generate())).isFalse();
    }

    @Test
    void verifyApiKey_returnsFalseForNonExistentUser() {
        final String userId = "user-999";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThat(userService.verifyApiKey(userId, ApiKeyUtils.generate())).isFalse();
    }
}