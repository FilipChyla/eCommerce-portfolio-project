package io.github.filipchyla.shopapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.dto.ChangePasswordRequest;
import io.github.filipchyla.shopapi.user.dto.PatchUserRequest;
import io.github.filipchyla.shopapi.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "h2"})
@Transactional
class ShopApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String EMAIL = "integration@example.com";
    private static final String PASSWORD = "Password123!";
    private static final String FIRSTNAME = "Integration";
    private static final String LASTNAME = "User";
    private static final String PHONE = "+48987654321";


    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = new User();
        user.setEmail(EMAIL);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setFirstName(FIRSTNAME);
        user.setLastName(LASTNAME);
        user.setPhone(PHONE);
        user.setEnabled(true);

        user = userRepository.save(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(new UserPrincipal(user), null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getMe_ShouldReturnCurrentUser() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.firstName").value(FIRSTNAME))
                .andExpect(jsonPath("$.lastName").value(LASTNAME))
                .andExpect(jsonPath("$.phone").value(PHONE));
    }

    @Test
    void updateMe_ShouldUpdateUser_WhenUserExists() throws Exception {
        String newFirstName = "NewName";
        String newLastName = "NewLastName";
        String newPhone = "+48123456789";

        PatchUserRequest patchRequest = new PatchUserRequest(newFirstName, newLastName, newPhone);

        mockMvc.perform(patch("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(newFirstName))
                .andExpect(jsonPath("$.lastName").value(newLastName))
                .andExpect(jsonPath("$.phone").value(newPhone));
    }

    @Test
    void updateMe_ShouldClearInfo_WhenRequestHasEmptyStrings() throws Exception {
        PatchUserRequest patchRequest = new PatchUserRequest("", "", "");

        mockMvc.perform(patch("/api/v1/user/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value(""))
                .andExpect(jsonPath("$.lastName").value(""))
                .andExpect(jsonPath("$.phone").value(""));
    }

    @Test
    void updatePassword_ShouldChangePassword_WhenCurrentPasswordIsCorrect() throws Exception {
        String newPassword = "NewPassword123!";
        ChangePasswordRequest request = new ChangePasswordRequest(PASSWORD, newPassword);

        mockMvc.perform(patch("/api/v1/user/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Password changed successfully"));
    }

    @Test
    void updatePassword_ShouldReturnError_WhenCurrentPasswordIsIncorrect() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword123!", "NewPassword123!");

        mockMvc.perform(patch("/api/v1/user/me/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMe_ShouldDeactivateCurrentUser() throws Exception {
        mockMvc.perform(delete("/api/v1/user/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("User deactivated successfully"));

        assertThat(userRepository.findByEmail(EMAIL))
                .isPresent()
                .get()
                .extracting(User::isEnabled)
                .isEqualTo(false);
    }
}
