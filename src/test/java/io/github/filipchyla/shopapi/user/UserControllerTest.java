package io.github.filipchyla.shopapi.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.auth.service.AuthenticationService;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.dto.ChangePasswordRequest;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.Authentication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String FIRSTNAME = "John";
    private static final String LASTNAME = "Doe";
    private static final String EMAIL = "test@email.com";
    private static final String PHONE = "+48123456789";

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);

        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMe_ShouldReturnUser_WhenUserExist() throws Exception {
        // Given
        UserResponse response = new UserResponse(
                USER_ID,
                EMAIL,
                FIRSTNAME,
                LASTNAME,
                PHONE,
                Instant.now()
        );

        when(userService.getUser(USER_ID)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(LASTNAME))
                .andExpect(jsonPath("$.phone").value(PHONE));
        verify(userService).getUser(USER_ID);
    }

    @Test
    void patchMe_ShouldReturnUpdatedUser_WhenRequestIsValid() throws Exception {
        // Given
        PatchUserRequest request =
                new PatchUserRequest(
                        FIRSTNAME,
                        LASTNAME,
                        PHONE
                );

        UserResponse response = new UserResponse(
                USER_ID,
                EMAIL,
                FIRSTNAME,
                LASTNAME,
                PHONE,
                Instant.now()
        );

        when(userService.patchUser(request, USER_ID)).thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(LASTNAME))
                .andExpect(jsonPath("$.phone").value(PHONE));
        verify(userService).patchUser(request, USER_ID);
    }

    @Test
    void patchMe_ShouldUpdateOnlyProvidedFields_WhenRequestIsPartial() throws Exception {
        // Given
        PatchUserRequest request = new PatchUserRequest(null, null, null);
        UserResponse response = new UserResponse(
                USER_ID,
                EMAIL,
                FIRSTNAME,
                LASTNAME,
                PHONE,
                Instant.now()
        );

        when(userService.patchUser(request, USER_ID)).thenReturn(response);

        // When & Then
        mockMvc.perform(patch("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(LASTNAME))
                .andExpect(jsonPath("$.phone").value(PHONE));
        verify(userService).patchUser(request, USER_ID);
    }

    @Test
    void patchMe_ShouldReturnBadRequest_WhenRequestIsInvalid() throws Exception {
        // Given
        String tooLong = "a".repeat(51);
        PatchUserRequest request =
                new PatchUserRequest(
                        tooLong,
                        tooLong,
                        "not-a-phone"
                );

        // When & Then
        mockMvc.perform(patch("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("firstName")))
                .andExpect(jsonPath("$.message").value(containsString("lastName")))
                .andExpect(jsonPath("$.message").value(containsString("phone")));
        verify(userService, never()).patchUser(any(), any());
    }

    @Test
    void updatePassword_ShouldUpdatePassword_WhenRequestIsValid() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPassword",
                "newPassword1!"
        );

        // When & Then
        mockMvc.perform(patch("/api/v1/user/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("Password changed successfully")));
        verify(authenticationService).changePassword(USER_ID, request);
    }

    @Test
    void updatePassword_ShouldReturnBadRequest_WhenNewPasswordIsWeak() throws Exception {
        // Given
        ChangePasswordRequest request = new ChangePasswordRequest(
                "currentPassword",
                "pass"
        );

        // When & Then
        mockMvc.perform(patch("/api/v1/user/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Password must contain an uppercase letter")))
                .andExpect(jsonPath("$.message").value(containsString("Password must contain a digit")))
                .andExpect(jsonPath("$.message").value(containsString("Password must contain a special character")))
                .andExpect(jsonPath("$.message").value(containsString("Password must be at least 8 characters")));
        verify(authenticationService, never()).changePassword(USER_ID, request);
    }

    @Test
    void deleteMe_ShouldDeactivateUser_WhenHeExists() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("User deactivated successfully")));
        verify(userService).deactivateUser(USER_ID);
    }
}