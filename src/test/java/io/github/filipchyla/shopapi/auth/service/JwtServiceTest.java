package io.github.filipchyla.shopapi.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceTest {

    private JwtService jwtService;
    private final String SECRET_KEY = Base64.getEncoder().encodeToString("super_secret_key_for_testing_purposes_only_1234567890".getBytes());
    private final String USERNAME = "test@example.com";

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        long expiration_time = 1000L * 60 * 60;
        jwtService = new JwtService(SECRET_KEY, expiration_time);

        userDetails = mock(UserDetails.class);

        when(userDetails.getUsername()).thenReturn(USERNAME);
    }

    @Test
    void generateToken_ShouldReturnValidToken() {
        // When
        String token = jwtService.generateToken(userDetails);

        // Then
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_ShouldReturnCorrectUsername() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(USERNAME);
    }

    @Test
    void extractUsername_ShouldThrowException_WhenTokenIsInvalid() {
        // When & Then
        assertThatThrownBy(() -> jwtService.extractUsername("invalid_token")).isInstanceOf(JwtException.class);
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenIsForUser() {
        // Given
        String token = jwtService.generateToken(userDetails);

        // When
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        // Given
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = mock(UserDetails.class);
        when(otherUser.getUsername()).thenReturn("other@example.com");

        // When
        boolean isValid = jwtService.isTokenValid(token, otherUser);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_ShouldThrowException_WhenTokenIsExpired() {
        // Given
        JwtService expiredJwtService = new JwtService(
                SECRET_KEY,
                -1000L
        );

        String token = expiredJwtService.generateToken(userDetails);

        // When & Then
        assertThatThrownBy(() -> expiredJwtService.isTokenValid(token, userDetails)).isInstanceOf(ExpiredJwtException.class);
    }
}
