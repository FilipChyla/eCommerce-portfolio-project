package io.github.filipchyla.shopapi.security.config;

import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.user.controller.UserController;
import io.github.filipchyla.shopapi.user.service.UserService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {UserController.class})
@Import(SecurityConfiguration.class)
@AutoConfigureMockMvc
public class SecurityConfigurationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    void protectedEndpoints_ShouldReturnUnauthorized_WhenNoAuthentication(
            HttpMethod method, String url) throws Exception {
        mockMvc.perform(request(method, url))
                .andExpect(status().isUnauthorized());
    }

    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.PATCH, "/api/v1/user/me")
        );
    }
}
