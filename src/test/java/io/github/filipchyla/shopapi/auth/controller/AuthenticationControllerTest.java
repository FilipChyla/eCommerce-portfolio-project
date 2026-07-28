package io.github.filipchyla.shopapi.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationResponse;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.auth.exception.EmailTakenException;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    private static final String VALID_EMAIL = "tes@email.com";
    private static final String VALID_PASSWORD = "Password123!";
    private static final String TOKEN = "jwt-token";

    @Nested
    class RegisterTests{
        @Test
        void register_ShouldReturnOk_WhenRequestIsValid() throws Exception {
            RegisterRequest request = new RegisterRequest(VALID_EMAIL, VALID_PASSWORD);
            AuthenticationResponse response = new AuthenticationResponse(TOKEN);

            when(authenticationService.register(request)).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN));
            verify(authenticationService).register(request);
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
    }


    @Nested
    class AuthenticateTests{
        @Test
        void authenticate_ShouldReturnOk_WhenCredentialsAreValid() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest(VALID_EMAIL, VALID_PASSWORD);
            AuthenticationResponse response = new AuthenticationResponse(TOKEN);

            when(authenticationService.authenticate(request)).thenReturn(response);

            mockMvc.perform(post("/api/v1/auth/authenticate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value(TOKEN));
            verify(authenticationService).authenticate(request);
        }

        @Test
        void authenticate_ShouldReturnUnauthorized_WhenCredentialsAreWrong() throws Exception {
            AuthenticationRequest request = new AuthenticationRequest(VALID_EMAIL, VALID_PASSWORD);

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
}
