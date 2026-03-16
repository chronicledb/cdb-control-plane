package io.github.grantchen2003.cdb.control.plane.users;

import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        final User user = new User(
                UUID.randomUUID().toString(),
                email,
                System.currentTimeMillis()
        );

        userRepository.save(user);

        return user;
    }
}