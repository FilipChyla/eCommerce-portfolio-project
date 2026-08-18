package io.github.filipchyla.shopapi.security.config;

import io.github.filipchyla.shopapi.auth.RefreshTokenCookieFactory;
import io.github.filipchyla.shopapi.auth.controller.AuthenticationController;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.auth.service.RefreshTokenService;
import io.github.filipchyla.shopapi.product.category.CategoryController;
import io.github.filipchyla.shopapi.product.category.CategoryMapper;
import io.github.filipchyla.shopapi.product.category.CategoryService;
import io.github.filipchyla.shopapi.user.controller.UserController;
import io.github.filipchyla.shopapi.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class, AuthenticationController.class, CategoryController.class})
@Import(SecurityConfiguration.class)
@AutoConfigureMockMvc
public class CsrfConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthenticationService authenticationService;
    @MockitoBean
    private RefreshTokenService refreshTokenService;
    @MockitoBean
    private RefreshTokenCookieFactory refreshTokenCookieFactory;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private CategoryMapper categoryMapper;

    @ParameterizedTest
    @MethodSource("csrfProtectedEndpoints")
    void cookieBasedEndpoints_ShouldReturnForbidden_WhenCsrfTokenMissing(
            HttpMethod method, String url) throws Exception {
        mockMvc.perform(request(method, url))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @MethodSource("csrfProtectedEndpoints")
    void cookieBasedEndpoints_ShouldNotReturnForbidden_WhenCsrfTokenPresent(
            HttpMethod method, String url) throws Exception {
        mockMvc.perform(request(method, url).with(csrf()))
                .andExpect(status().is(not(403)));
    }

    @Test
    void headerBasedEndpoints_ShouldNotRequireCsrf() throws Exception {
        mockMvc.perform(get("/api/v1/user/me")
                        .with(user("testuser").roles("USER")))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }

    static Stream<Arguments> csrfProtectedEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.POST, "/api/v1/auth/refresh"),
                Arguments.of(HttpMethod.POST, "/api/v1/auth/logout")
        );
    }
}