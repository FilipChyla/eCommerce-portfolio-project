package io.github.filipchyla.shopapi.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.auth.service.JwtService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.dto.UserResponse;
import io.github.filipchyla.shopapi.user.service.UserService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private JwtService jwtService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final String FIRSTNAME = "John";
    private static final String LASTNAME = "Doe";
    private static final String EMAIL = "test@email.com";
    private static final String PHONE = "+48123456789";
    private Authentication auth;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(USER_ID);

        auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
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
                LocalDateTime.now()
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
                LocalDateTime.now()
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
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("firstName")))
                .andExpect(jsonPath("$.message").value(containsString("lastName")))
                .andExpect(jsonPath("$.message").value(containsString("phone")));
        verify(userService, never()).patchUser(any(), any());
    }
}
