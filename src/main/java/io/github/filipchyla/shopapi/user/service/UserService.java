package io.github.filipchyla.shopapi.user.service;

import io.github.filipchyla.shopapi.auth.exception.EmailTakenException;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import io.github.filipchyla.shopapi.user.exception.UserNotFoundException;
import io.github.filipchyla.shopapi.user.mapper.UserMapper;
import io.github.filipchyla.shopapi.user.repository.UserRepository;
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

    public UserResponse getUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return userMapper.toResponse(user);
    }

    public User getUserEntity(UUID userId){
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
    }

    @Transactional
    public UserResponse patchUser(PatchUserRequest request, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        userMapper.updateFromPatchRequest(request, user);

        return userMapper.toResponse(user);
    }

    @Transactional
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        user.setEnabled(false);
    }
}
