package io.github.filipchyla.shopapi.security.config.authorization;

import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.user.controller.UserController;
import io.github.filipchyla.shopapi.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
class UserAuthorizationTest extends AuthorizationTest{
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthenticationService authenticationService;

    @Test
    void getMe_IsNotAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_IsAccessible_WhenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }

    @Test
    void changePassword_IsNotAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/user/me/password"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_IsAccessible_WhenAuthenticated() throws Exception {
        mockMvc.perform(patch("/api/v1/user/me/password")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }
}