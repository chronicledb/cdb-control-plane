package io.github.grantchen2003.cdb.control.plane.users;

import io.github.grantchen2003.cdb.control.plane.utils.ApiKeyUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

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
}