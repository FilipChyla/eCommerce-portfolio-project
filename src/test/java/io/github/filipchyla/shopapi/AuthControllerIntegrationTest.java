package io.github.filipchyla.shopapi;

import io.github.filipchyla.shopapi.role.Role;
import io.github.filipchyla.shopapi.role.RoleName;
import io.github.filipchyla.shopapi.role.RoleRepository;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles({"test", "testcontainers"})
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MockMvc mockMvc;

    private static final String LOGIN_BODY = """
                {"email":"test@example.com","password":"correct-password"}
            """;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shopdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAllInBatch();

        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash(passwordEncoder.encode("correct-password"));
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEnabled(true);

        Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();
        user.setRole(userRole);

        userRepository.save(user);
    }

    @Test
    void register_SetsRefreshCookieAndReturnsAccessToken_WhenCredentialsAreCorrect() throws Exception {
        String registerBody = """
                    {"email":"test@register.com","password":"StrongPassword123!"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void register_ShouldReturnBadCredentials_WhenCredentialsAreIncorrect() throws Exception {
        String registerBody = """
                    {"email":"bademail","password":"pass"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(cookie().doesNotExist("refreshToken"));
    }

    @Test
    void register_ShouldReturnConflict_WhenEmailAlreadyUsed() throws Exception {
        String registerBody = """
                    {"email":"test@register.com","password":"StrongPassword123!"}
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(cookie().doesNotExist("refreshToken"));
    }

    @Test
    void login_SetsRefreshCookieAndReturnsAccessToken_WhenCredentialsAreCorrect() throws Exception {
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenCredentialsAreIncorrect() throws Exception {
        String loginBody = """
                    {"email":"bademail","password":"pass"}
                """;
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(cookie().doesNotExist("refreshToken"));
    }

    @Test
    void refresh_ShouldRotateCookieAndReturnsNewAccessToken_WhenCookieIsCorrect() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andReturn();

        Cookie oldCookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(oldCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true));
    }

    @Test
    void refresh_ShouldReturnUnauthorized_WhenCookieIsIncorrect() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refreshToken", "incorrect-cookie")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(cookie().doesNotExist("refreshToken"));
    }

    @Test
    void refresh_ReturnBadRequest_WhenCookieIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(cookie().doesNotExist("refreshToken"));
    }

    @Test
    void refresh_ShouldReturnUnauthorized_WhenCookieIsReused() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andReturn();
        Cookie oldCookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(csrf())
                .cookie(oldCookie));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(oldCookie))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_ClearsCookieAndInvalidatesToken() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andReturn();
        Cookie cookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(cookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(cookie))
                .andExpect(status().isUnauthorized());
    }
}
