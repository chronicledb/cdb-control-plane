package io.github.grantchen2003.cdb.control.plane.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        final User user = userService.createUser("alice@example.com");

        assertThat(user.email()).isEqualTo("alice@example.com");
        assertThat(user.id()).isNotNull();
        assertThat(user.createdAt()).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_duplicateEmail_throwsException() {
        final User existing = new User("some-id", "alice@example.com", Instant.now());
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> userService.createUser("alice@example.com"))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }
}