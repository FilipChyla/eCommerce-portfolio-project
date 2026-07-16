package io.github.filipchyla.shopapi.user;

import io.github.filipchyla.shopapi.shared.exception.EmailTakenException;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import io.github.filipchyla.shopapi.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User createUser(String email, String passwordHash) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailTakenException(email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        return userRepository.save(user);
    }

    @Transactional
    public UserResponse patchUser(PatchUserRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        userMapper.updateFromPatchRequest(request, user);

        return userMapper.toResponse(user);
    }
}
