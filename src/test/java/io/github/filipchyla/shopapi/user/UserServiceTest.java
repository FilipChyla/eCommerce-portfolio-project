package io.github.filipchyla.shopapi.user;

import io.github.filipchyla.shopapi.auth.exception.EmailTakenException;
import io.github.filipchyla.shopapi.role.Role;
import io.github.filipchyla.shopapi.role.RoleName;
import io.github.filipchyla.shopapi.role.RoleRepository;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    public static final String EMAIL = "test@example.com";

    @Test
    void createUser_ShouldSaveUser_WhenEmailIsNotTaken() {
        // Given
        String passwordHash = "hashed_password";

        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(new Role()));

        // When
        User result = userService.createUser(EMAIL, passwordHash);

        // Then
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getPasswordHash()).isEqualTo(passwordHash);
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_ShouldThrowEmailTakenException_WhenEmailIsTaken() {
        // Given
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(EMAIL, "password"))
                .isInstanceOf(EmailTakenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUser_ShouldReturnUser_WhenUserExists() {
        // Given
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);

        UserResponse response = new UserResponse(userId, user.getEmail(), "John", "Doe", "+48123456789", Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        // When
        UserResponse result = userService.getUser(userId);

        // Then
        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(userId);
        verify(userMapper).toResponse(user);
    }

    @Test
    void getUserEntity_ShouldReturnUserEntity_WhenUserExists() {
        // Given
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        User foundUser = userService.getUserEntity(userId);

        // Then
        assertThat(foundUser).isEqualTo(user);
        verify(userRepository).findById(userId);
    }

    @Test
    void patchUser_ShouldUpdateUser_WhenUserExists() {
        // Given
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setEmail(EMAIL);

        PatchUserRequest request = new PatchUserRequest("John", "Doe", "+48123456789");
        UserResponse response = new UserResponse(userId, user.getEmail(), "John", "Doe", "+48123456789", Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        // When
        UserResponse result = userService.patchUser(request, userId);

        // Then
        assertThat(result).isEqualTo(response);
        verify(userRepository).findById(userId);
        verify(userMapper).updateFromPatchRequest(request, user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void patchUser_ShouldThrowUserNotFoundException_WhenUserDoesNotExist() {
        // Given
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.patchUser(mock(PatchUserRequest.class), userId))
                .isInstanceOf(UserNotFoundException.class);
        verify(userMapper, never()).updateFromPatchRequest(any(), any());
    }

    @Test
    void deactivateUser_ShouldSetEnabledToFalse_WhenUserExists() {
        // Given
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setEnabled(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        userService.deactivateUser(userId);

        // Then
        assertFalse(user.isEnabled());
    }
}
