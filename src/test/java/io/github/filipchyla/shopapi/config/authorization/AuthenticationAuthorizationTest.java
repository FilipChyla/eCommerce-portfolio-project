package io.github.filipchyla.shopapi.config.authorization;

import io.github.filipchyla.shopapi.auth.RefreshTokenCookieFactory;
import io.github.filipchyla.shopapi.auth.AuthenticationController;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.RefreshTokenService;
import io.github.filipchyla.shopapi.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc
class AuthenticationAuthorizationTest extends AuthorizationTest{
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthenticationService authenticationService;
    @MockitoBean
    private RefreshTokenService refreshTokenService;
    @MockitoBean
    private RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Test
    void register_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void logoutAll_IsNotAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAll_IsAccessible_WhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout-all")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }
}