package io.github.filipchyla.shopapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationRequest;
import io.github.filipchyla.shopapi.auth.dto.AuthenticationResponse;
import io.github.filipchyla.shopapi.auth.dto.RegisterRequest;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShopApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String EMAIL = "integration@example.com";
    private static final String PASSWORD = "Password123!";
    private static final String FIRSTNAME = "Integration";
    private static final String LASTNAME = "User";
    private static final String PHONE = "+48987654321";

    @Test
    void registerWithProfileUpdateFlow() throws Exception {
        // 1. Register
        RegisterRequest registerRequest = new RegisterRequest(EMAIL, PASSWORD);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());

        // 2. Authenticate
        AuthenticationRequest authRequest = new AuthenticationRequest(EMAIL, PASSWORD);
        MvcResult authResult = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String token = objectMapper.readValue(authResult.getResponse().getContentAsString(), AuthenticationResponse.class).token();

        // 3. Update profile
        PatchUserRequest patchRequest = new PatchUserRequest(FIRSTNAME, LASTNAME, PHONE);
        mockMvc.perform(patch("/api/v1/user/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(LASTNAME))
                .andExpect(jsonPath("$.phone").value(PHONE));
    }
}
