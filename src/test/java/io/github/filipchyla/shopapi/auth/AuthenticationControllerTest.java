package io.github.filipchyla.shopapi.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.auth.exception.EmailTakenException;
import io.github.filipchyla.shopapi.auth.exception.InvalidRefreshTokenException;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.auth.service.RefreshTokenService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "app.refresh-token.cookie-name=refreshToken")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private RefreshTokenService refreshTokenService;
    @MockitoBean
    private RefreshTokenCookieFactory cookieFactory;
    @MockitoBean
    private UserService userService;

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
    class RegisterRequestTest {
        @Test
        void register_ShouldReturnAccessTokenAndSetRefreshCookie_WhenRequestIsValid() throws Exception {
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, VALID_PASSWORD);
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, REFRESH_TOKEN)
                    .httpOnly(true)
                    .path("/")
                    .build();

            when(authenticationService.register(any(RegisterRequest.class))).thenReturn(userPrincipal);
            when(jwtService.generateToken(userPrincipal)).thenReturn(JWT_TOKEN);
            when(refreshTokenService.createNewToken(user.getId().toString())).thenReturn(REFRESH_TOKEN);
            when(cookieFactory.create(REFRESH_TOKEN)).thenReturn(cookie);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.token").value(JWT_TOKEN))
                    .andExpect(cookie().exists(COOKIE_NAME))
                    .andExpect(cookie().value(COOKIE_NAME, REFRESH_TOKEN))
                    .andExpect(cookie().httpOnly(COOKIE_NAME, true))
                    .andExpect(cookie().path(COOKIE_NAME, "/"));

            verify(authenticationService).register(any(RegisterRequest.class));
            verify(jwtService).generateToken(userPrincipal);
            verify(refreshTokenService).createNewToken(user.getId().toString());
        }

        @Test
        void register_ShouldReturnConflict_WhenEmailIsTaken() throws Exception {
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, VALID_PASSWORD);

            when(authenticationService.register(request)).thenThrow(EmailTakenException.class);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        void register_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
            RegisterRequest request = new RegisterRequest("invalid-email", VALID_PASSWORD);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authenticationService);
        }

        @Test
        void register_ShouldReturnBadRequest_WhenPasswordIsWeak() throws Exception {
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, "pass");

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Password must contain an uppercase letter")))
                    .andExpect(jsonPath("$.message").value(containsString("Password must contain a digit")))
                    .andExpect(jsonPath("$.message").value(containsString("Password must contain a special character")))
                    .andExpect(jsonPath("$.message").value(containsString("Password must be at least 8 characters")));
            verifyNoInteractions(authenticationService);
        }

        @Test
        void register_ShouldReturnBadRequest_WhenBodyInvalid() throws Exception {
            String invalidJson = "{}";

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class AuthenticationRequestTest {
        @Test
        void authenticate_ShouldReturnAccessTokenAndSetRefreshCookie_WhenRequestIsValid() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest(VALID_EMAIL, VALID_PASSWORD);
            ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, REFRESH_TOKEN)
                    .httpOnly(true)
                    .path("/")
                    .build();

            when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(userPrincipal);
            when(jwtService.generateToken(userPrincipal)).thenReturn(JWT_TOKEN);
            when(refreshTokenService.createNewToken(user.getId().toString())).thenReturn(REFRESH_TOKEN);
            when(cookieFactory.create(REFRESH_TOKEN)).thenReturn(cookie);

            mockMvc.perform(post("/api/v1/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(JWT_TOKEN))
                    .andExpect(cookie().exists(COOKIE_NAME))
                    .andExpect(cookie().value(COOKIE_NAME, REFRESH_TOKEN))
                    .andExpect(cookie().httpOnly(COOKIE_NAME, true))
                    .andExpect(cookie().path(COOKIE_NAME, "/"));
        }

        @Test
        void authenticate_ShouldReturnUnauthorized_WhenCredentialsAreWrong() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest("invalid@mail.com", "VeryWrongPassword1!");

            when(authenticationService.authenticate(request)).thenThrow(BadCredentialsException.class);

            mockMvc.perform(post("/api/v1/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void authenticate_ShouldReturnBadRequest_WhenCredentialsAreInvalid() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest("", "");

            mockMvc.perform(post("/api/v1/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("Password should not be blank")))
                    .andExpect(jsonPath("$.message").value(containsString("Email should not be blank")));
            verifyNoInteractions(authenticationService);
        }
    }

    @Nested
    class RefreshTokenCookieTest {
        @Test
        void refresh_ShouldRotateTokenAndReturnNewAccessToken_WhenRequestIsValid() throws Exception {
            String oldRawToken = "old-raw-refresh-token";
            String newRawToken = "new-raw-refresh-token";
            ResponseCookie newCookie = ResponseCookie.from(COOKIE_NAME, newRawToken)
                    .httpOnly(true)
                    .path("/")
                    .build();

            when(refreshTokenService.rotateToken(oldRawToken)).thenReturn(newRawToken);
            when(refreshTokenService.getUserIdFromToken(oldRawToken)).thenReturn(user.getId().toString());
            when(userService.getUserEntity(user.getId())).thenReturn(user);
            when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("new-access-token");
            when(cookieFactory.create(newRawToken)).thenReturn(newCookie);

            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie(COOKIE_NAME, oldRawToken)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("new-access-token"))
                    .andExpect(cookie().exists(COOKIE_NAME))
                    .andExpect(cookie().value(COOKIE_NAME, newRawToken))
                    .andExpect(cookie().httpOnly(COOKIE_NAME, true))
                    .andExpect(cookie().path(COOKIE_NAME, "/"));

            verify(refreshTokenService).rotateToken(oldRawToken);
        }

        @Test
        void refresh_ShouldNotRotateTokenAndReturnNewAccessToken_WhenCookieIsWrongOrExpired() throws Exception {
            String oldRawToken = "old-raw-refresh-token";

            when(refreshTokenService.getUserIdFromToken(oldRawToken)).thenReturn(user.getId().toString());
            when(refreshTokenService.rotateToken(oldRawToken)).thenThrow(InvalidRefreshTokenException.class);
            when(userService.getUserEntity(user.getId())).thenReturn(user);


            mockMvc.perform(post("/api/v1/auth/refresh")
                            .cookie(new jakarta.servlet.http.Cookie(COOKIE_NAME, oldRawToken)))
                    .andExpect(status().isUnauthorized());

            verify(refreshTokenService).rotateToken(oldRawToken);
            verify(jwtService, never()).generateToken(any());
            verify(cookieFactory, never()).create(any());
        }

        @Test
        void refresh_ShouldReturnBadRequest_WhenCookieMissing() throws Exception {
            mockMvc.perform(post("/api/v1/auth/refresh"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class LogoutTest {
        @Test
        void logout_ShouldRevokeTokenAndClearCookie_WhenCookiePresent() throws Exception {
            String rawToken = REFRESH_TOKEN;
            ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "")
                    .maxAge(0)
                    .path("/")
                    .build();

            when(cookieFactory.createExpired()).thenReturn(expiredCookie);

            mockMvc.perform(post("/api/v1/auth/logout")
                            .cookie(new jakarta.servlet.http.Cookie(COOKIE_NAME, rawToken)))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists(COOKIE_NAME))
                    .andExpect(cookie().maxAge(COOKIE_NAME, 0))
                    .andExpect(cookie().path(COOKIE_NAME, "/"));

            verify(refreshTokenService).revokeToken(rawToken);
        }

        @Test
        void logout_ShouldNotRevoke_WhenCookieMissing() throws Exception {
            ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "")
                    .maxAge(0)
                    .path("/")
                    .build();

            when(cookieFactory.createExpired()).thenReturn(expiredCookie);

            mockMvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists(COOKIE_NAME))
                    .andExpect(cookie().maxAge(COOKIE_NAME, 0));

            verify(refreshTokenService, org.mockito.Mockito.never()).revokeToken(org.mockito.ArgumentMatchers.anyString());
        }


        @Test
        void logoutAll_ShouldRevokeAllTokensForUser_WhenUserIsAuthenticated() throws Exception {
            ResponseCookie expiredCookie = ResponseCookie.from(COOKIE_NAME, "")
                    .maxAge(0)
                    .path("/")
                    .build();

            Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(cookieFactory.createExpired()).thenReturn(expiredCookie);

            mockMvc.perform(post("/api/v1/auth/logout-all"))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists(COOKIE_NAME))
                    .andExpect(cookie().maxAge(COOKIE_NAME, 0));

            verify(refreshTokenService).revokeAllForUser(user.getId().toString());
        }
    }
}