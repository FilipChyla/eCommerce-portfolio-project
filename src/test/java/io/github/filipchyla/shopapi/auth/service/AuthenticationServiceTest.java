package io.github.filipchyla.shopapi.auth.service;

import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationResponse;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.auth.exception.EmailTakenException;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.dto.ChangePasswordRequest;
import io.github.filipchyla.shopapi.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationService authenticationService;

    private static final String EMAIL = "test@example.com";
    private static final String RAW_PASSWORD = "Password123";
    private static final String ENCODED_PASSWORD = "encodedPassword";
    private static final String TOKEN = "jwt_token";

    @Test
    void register_ShouldReturnResponseWithToken_WhenSuccessful() {
        // Given
        RegisterRequest request = new RegisterRequest(EMAIL, RAW_PASSWORD);
        User user = new User();
        user.setEmail(request.email());

        when(passwordEncoder.encode(request.password())).thenReturn(ENCODED_PASSWORD);
        when(userService.createUser(request.email(), ENCODED_PASSWORD)).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn(TOKEN);

        // When
        AuthenticationResponse response = authenticationService.register(request);

        // Then
        assertThat(response.token()).isEqualTo(TOKEN);
        verify(passwordEncoder).encode(request.password());
        verify(userService).createUser(request.email(), ENCODED_PASSWORD);
        verify(jwtService).generateToken(any());
    }

    @Test
    void register_ShouldThrowException_WhenEmailIsTaken() {
        // Given
        RegisterRequest request = new RegisterRequest(EMAIL, RAW_PASSWORD);

        when(passwordEncoder.encode(request.password())).thenReturn(ENCODED_PASSWORD);
        when(userService.createUser(request.email(), ENCODED_PASSWORD)).thenThrow(EmailTakenException.class);

        // When & Then
        assertThatThrownBy(() -> authenticationService.register(request)).isInstanceOf(EmailTakenException.class);
        verify(passwordEncoder).encode(request.password());
        verify(userService).createUser(request.email(), ENCODED_PASSWORD);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void authenticate_ShouldReturnResponseWithToken_WhenCredentialsAreValid() {
        // Given
        AuthenticationRequest request = new AuthenticationRequest(EMAIL, RAW_PASSWORD);
        UserDetails userDetails = mock(UserDetails.class);

        when(userDetailsService.loadUserByUsername(request.email())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn(TOKEN);

        // When
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Then
        assertThat(response.token()).isEqualTo(TOKEN);
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userDetailsService).loadUserByUsername(request.email());
        verify(jwtService).generateToken(userDetails);
    }

    @Test
    void authenticate_ShouldThrowException_WhenCredentialsAreInvalid() {
        // Given
        AuthenticationRequest request = new AuthenticationRequest(EMAIL, RAW_PASSWORD);

        when(authenticationManager.authenticate(any())).thenThrow(BadCredentialsException.class);

        // When & Then
        assertThatThrownBy(() -> authenticationService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);
        verify(userDetailsService, never()).loadUserByUsername(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void changePassword_ShouldChangePassword_WhenCurrentPasswordIsCorrect() {
        // Given
        String newRawPassword = "newPassword";
        ChangePasswordRequest request = new ChangePasswordRequest(RAW_PASSWORD, newRawPassword);

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setPasswordHash(ENCODED_PASSWORD);

        when(userService.getUserEntity(userId)).thenReturn(user);
        when(passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.encode(newRawPassword)).thenReturn("NewPasswordHash");

        // When
        authenticationService.changePassword(userId, request);

        // Then
        assertThat(user.getPasswordHash()).isEqualTo("NewPasswordHash");
        verify(passwordEncoder).encode(request.newPassword());
    }

    @Test
    void changePassword_ShouldThrowException_WhenCurrentPasswordIsIncorrect() {
        // Given
        String newRawPassword = "newPassword";
        ChangePasswordRequest request = new ChangePasswordRequest(RAW_PASSWORD, newRawPassword);

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setPasswordHash(ENCODED_PASSWORD);

        when(userService.getUserEntity(userId)).thenReturn(user);
        when(passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())).thenReturn(false);

        // When & Then
        assertThatThrownBy(()->authenticationService.changePassword(userId, request)).isInstanceOf(BadCredentialsException.class);
        assertThat(user.getPasswordHash()).isEqualTo(ENCODED_PASSWORD);
    }
}
