package io.github.filipchyla.shopapi.auth;

import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationTokensData;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.auth.exception.EmailTakenException;
import io.github.filipchyla.shopapi.auth.exception.InvalidRefreshTokenException;
import io.github.filipchyla.shopapi.auth.exception.UserDisabledException;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.auth.service.RefreshTokenService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFacadeTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private RefreshTokenCookieFactory cookieFactory;
    @Mock
    private UserService userService;

    @InjectMocks
    private AuthenticationFacade authenticationFacade;

    private User user;
    private UserPrincipal userPrincipal;

    private static final String VALID_EMAIL = "tes@email.com";
    private static final String VALID_PASSWORD = "Password123!";
    private static final String JWT_TOKEN = "jwt-token";
    private static final String REFRESH_TOKEN = "raw-refresh-token";
    private static final String COOKIE_NAME = "refreshToken";

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        userPrincipal = new UserPrincipal(user);
    }

    @Nested
    class RegisterUserAndReturnTokensTest {

        @Test
        void shouldReturnAccessTokenAndRefreshCookie_whenRegisterIsSuccessful() {
            // Given
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, VALID_PASSWORD);
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, REFRESH_TOKEN)
                    .httpOnly(true)
                    .path("/")
                    .build();

            when(authenticationService.register(request)).thenReturn(userPrincipal);
            when(jwtService.generateToken(userPrincipal)).thenReturn(JWT_TOKEN);
            when(refreshTokenService.createNewToken(user.getId().toString())).thenReturn(REFRESH_TOKEN);
            when(cookieFactory.create(REFRESH_TOKEN)).thenReturn(cookie);

            // When
            AuthenticationTokensData result = authenticationFacade.registerUserAndReturnTokens(request);

            // Then
            assertEquals(JWT_TOKEN, result.accessToken().token());
            assertEquals(cookie, result.refreshTokenCookie());
            verify(authenticationService).register(request);
            verify(refreshTokenService).createNewToken(user.getId().toString());
        }

        @Test
        void shouldPropagateException_whenEmailIsTaken() {
            // Given
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, VALID_PASSWORD);
            when(authenticationService.register(request)).thenThrow(EmailTakenException.class);

            // When & then
            assertThrows(EmailTakenException.class,
                    () -> authenticationFacade.registerUserAndReturnTokens(request));
            verifyNoInteractions(jwtService, refreshTokenService, cookieFactory);
        }
    }

    @Nested
    class AuthenticateUserAndReturnTokensTest {

        @Test
        void shouldReturnAccessTokenAndRefreshCookie_whenAuthenticationIsSuccessful() {
            // Given
            AuthenticationRequest request = new AuthenticationRequest(VALID_EMAIL, VALID_PASSWORD);
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, REFRESH_TOKEN)
                    .httpOnly(true)
                    .path("/")
                    .build();

            when(authenticationService.authenticate(request)).thenReturn(userPrincipal);
            when(jwtService.generateToken(userPrincipal)).thenReturn(JWT_TOKEN);
            when(refreshTokenService.createNewToken(user.getId().toString())).thenReturn(REFRESH_TOKEN);
            when(cookieFactory.create(REFRESH_TOKEN)).thenReturn(cookie);

            // When
            AuthenticationTokensData result = authenticationFacade.authenticateUserAndReturnTokens(request);

            // Then
            assertEquals(JWT_TOKEN, result.accessToken().token());
            assertEquals(cookie, result.refreshTokenCookie());
        }

        @Test
        void shouldPropagateException_whenCredentialsAreWrong() {
            // Given
            AuthenticationRequest request = new AuthenticationRequest("invalid@mail.com", "VeryWrongPassword1!");
            when(authenticationService.authenticate(request)).thenThrow(BadCredentialsException.class);

            // When & then
            assertThrows(BadCredentialsException.class,
                    () -> authenticationFacade.authenticateUserAndReturnTokens(request));
            verifyNoInteractions(jwtService, refreshTokenService, cookieFactory);
        }
    }

    @Nested
    class RefreshTokenTest {

        @Test
        void shouldRotateTokenAndReturnNewAccessToken_whenRequestIsValid() {
            // Given
            String oldRawToken = "old-raw-refresh-token";
            String newRawToken = "new-raw-refresh-token";
            String newAccessToken = "new-access-token";
            ResponseCookie newCookie = ResponseCookie.from(COOKIE_NAME, newRawToken)
                    .httpOnly(true)
                    .path("/")
                    .build();

            when(refreshTokenService.getUserIdFromToken(oldRawToken)).thenReturn(user.getId().toString());
            when(userService.getUserEntity(user.getId())).thenReturn(user);
            when(refreshTokenService.rotateToken(oldRawToken)).thenReturn(newRawToken);
            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn(newAccessToken);
            when(cookieFactory.create(newRawToken)).thenReturn(newCookie);

            // When
            AuthenticationTokensData result = authenticationFacade.refreshToken(oldRawToken);

            // Then
            assertEquals(newAccessToken, result.accessToken().token());
            assertEquals(newCookie, result.refreshTokenCookie());
            verify(refreshTokenService).rotateToken(oldRawToken);
        }

        @Test
        void shouldPropagateException_whenRefreshTokenIsInvalidOrExpired() {
            // Given
            String oldRawToken = "old-raw-refresh-token";

            when(refreshTokenService.getUserIdFromToken(oldRawToken)).thenReturn(user.getId().toString());
            when(userService.getUserEntity(user.getId())).thenReturn(user);
            when(refreshTokenService.rotateToken(oldRawToken)).thenThrow(InvalidRefreshTokenException.class);

            // When & then
            assertThrows(InvalidRefreshTokenException.class,
                    () -> authenticationFacade.refreshToken(oldRawToken));
            verify(jwtService, never()).generateToken(any());
            verify(cookieFactory, never()).create(anyString());
        }

        @Test
        void shouldThrowUserDisabledException_whenUserAccountIsDeactivated() {
            // Given
            String oldRawToken = "old-raw-refresh-token";
            user.setEnabled(false);

            when(refreshTokenService.getUserIdFromToken(oldRawToken)).thenReturn(user.getId().toString());
            when(userService.getUserEntity(user.getId())).thenReturn(user);

            // When & then
            assertThrows(UserDisabledException.class,
                    () -> authenticationFacade.refreshToken(oldRawToken));
            verify(refreshTokenService, never()).rotateToken(anyString());
            verify(jwtService, never()).generateToken(any());
        }
    }

    @Nested
    class RevokeSingleForUserTest {

        @Test
        void shouldRevokeTokenAndReturnExpiredCookie_whenTokenIsPresent() {
            // Given
            String rawToken = REFRESH_TOKEN;
            ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "")
                    .maxAge(0)
                    .path("/")
                    .build();
            when(cookieFactory.createExpired()).thenReturn(expiredCookie);

            // When
            ResponseCookie result = authenticationFacade.revokeSingleForUser(rawToken);

            // Then
            assertEquals(expiredCookie, result);
            verify(refreshTokenService).revokeToken(rawToken);
        }

        @Test
        void shouldNotRevokeAnything_whenTokenIsNull() {
            // Given
            ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "")
                    .maxAge(0)
                    .path("/")
                    .build();
            when(cookieFactory.createExpired()).thenReturn(expiredCookie);

            // When
            ResponseCookie result = authenticationFacade.revokeSingleForUser(null);

            // Then
            assertEquals(expiredCookie, result);
            verify(refreshTokenService, never()).revokeToken(anyString());
        }
    }

    @Nested
    class RevokeAllTokensForUserTest {

        @Test
        void shouldRevokeAllTokensForUserAndReturnExpiredCookie() {
            // Given
            ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "")
                    .maxAge(0)
                    .path("/")
                    .build();
            when(cookieFactory.createExpired()).thenReturn(expiredCookie);

            // When
            ResponseCookie result = authenticationFacade.revokeAllTokensForUser(userPrincipal);

            // Then
            assertEquals(expiredCookie, result);
            verify(refreshTokenService).revokeAllForUser(user.getId().toString());
        }
    }
}