package io.github.filipchyla.shopapi.product.category;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.role.Role;
import io.github.filipchyla.shopapi.role.RoleName;
import io.github.filipchyla.shopapi.role.RoleRepository;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles({"test", "testcontainers"})
@AutoConfigureMockMvc
class CategoryIntegrationTest {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CacheManager cacheManager;

    private String adminToken;
    private String userToken;

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
    void setUp() throws Exception {
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        if (cacheManager.getCache("categories") != null) {
            cacheManager.getCache("categories").clear();
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();

        User admin = new User();
        admin.setEmail("admin@example.com");
        admin.setPasswordHash(passwordEncoder.encode("AdminPassword123!"));
        admin.setFirstName("Admin");
        admin.setLastName("User");
        admin.setEnabled(true);
        admin.setRole(adminRole);
        userRepository.save(admin);

        User plainUser = new User();
        plainUser.setEmail("user@example.com");
        plainUser.setPasswordHash(passwordEncoder.encode("UserPassword123!"));
        plainUser.setFirstName("Plain");
        plainUser.setLastName("User");
        plainUser.setEnabled(true);
        plainUser.setRole(userRole);
        userRepository.save(plainUser);

        adminToken = obtainToken("admin@example.com", "AdminPassword123!");
        userToken = obtainToken("user@example.com", "UserPassword123!");
    }

    private String obtainToken(String email, String password) throws Exception {
        String loginBody = """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }

    private Category saveCategory(String name, Category parent) {
        Category category = new Category();
        category.setName(name);
        category.setParent(parent);
        return categoryRepository.save(category);
    }


    @Nested
    class GetCategories {
        @Test
        void getCategories_ReturnsEmptyList_WhenNoCategoriesExist() throws Exception {
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void getCategories_ReturnsNestedTree_WhenParentAndChildExist() throws Exception {
            Category parent = saveCategory("Electronics", null);
            saveCategory("Laptops", parent);

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Electronics"))
                    .andExpect(jsonPath("$[0].children", hasSize(1)))
                    .andExpect(jsonPath("$[0].children[0].name").value("Laptops"));
        }

        @Test
        void getCategories_DoesNotRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk());
        }
    }


    @Nested
    class AddCategory {
        @Test
        void addCategory_CreatesRootCategory_WhenAdminAndValidRequest() throws Exception {
            String body = """
                    {"name":"Electronics"}
                    """;

            mockMvc.perform(post("/api/v1/admin/categories")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Electronics"))
                    .andExpect(jsonPath("$.parentId").doesNotExist());
        }

        @Test
        void addCategory_CreatesSubcategory_WhenParentIdProvided() throws Exception {
            Category parent = saveCategory("Electronics", null);

            String body = """
                    {"name":"Laptops","parentId":"%s"}
                    """.formatted(parent.getId());

            mockMvc.perform(post("/api/v1/admin/categories")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Laptops"))
                    .andExpect(jsonPath("$.parentId").value(parent.getId().toString()));
        }

        @Test
        void addCategory_EvictsTreeCache_SoNewCategoryIsVisibleImmediately() throws Exception {
            mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());

            String body = """
                    {"name":"Electronics"}
                    """;

            mockMvc.perform(post("/api/v1/admin/categories")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].name").value("Electronics"));
        }

        @Test
        void addCategory_ReturnsBadRequest_WhenNameIsBlank() throws Exception {
            String body = """
                    {"name":""}
                    """;

            mockMvc.perform(post("/api/v1/admin/categories")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addCategory_ReturnsNotFound_WhenParentIdDoesNotExist() throws Exception {
            String body = """
                    {"name":"Laptops","parentId":"%s"}
                    """.formatted(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/admin/categories")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        void addCategory_ReturnsForbidden_WhenUserIsNotAdmin() throws Exception {
            String body = """
                    {"name":"Electronics"}
                    """;

            mockMvc.perform(post("/api/v1/admin/categories")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        void addCategory_ReturnsUnauthorized_WhenNoTokenProvided() throws Exception {
            String body = """
                    {"name":"Electronics"}
                    """;

            mockMvc.perform(post("/api/v1/admin/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }


    @Nested
    class UpdateCategory {
        @Test
        void updateCategory_UpdatesName_WhenAdminAndValidRequest() throws Exception {
            Category category = saveCategory("Electronics", null);

            String body = """
                    {"name":"Consumer Electronics"}
                    """;

            mockMvc.perform(patch("/api/v1/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Consumer Electronics"));
        }

        @Test
        void updateCategory_ReassignsParent_WhenParentIdProvided() throws Exception {
            Category oldParent = saveCategory("Electronics", null);
            Category newParent = saveCategory("Appliances", null);
            Category child = saveCategory("Laptops", oldParent);

            String body = """
                    {"parentId":"%s"}
                    """.formatted(newParent.getId());

            mockMvc.perform(patch("/api/v1/admin/categories/{id}", child.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.parentId").value(newParent.getId().toString()));
        }

        @Test
        void updateCategory_KeepsExistingName_WhenNameIsNull() throws Exception {
            Category category = saveCategory("Electronics", null);

            String body = """
                    {"parentId":null}
                    """;

            mockMvc.perform(patch("/api/v1/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Electronics"));
        }

        @Test
        void updateCategory_ReturnsBadRequest_WhenNameTooShort() throws Exception {
            Category category = saveCategory("Electronics", null);

            String body = """
                    {"name":"AB"}
                    """;

            mockMvc.perform(patch("/api/v1/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateCategory_ReturnsNotFound_WhenCategoryDoesNotExist() throws Exception {
            String body = """
                    {"name":"Doesn't matter"}
                    """;

            mockMvc.perform(patch("/api/v1/admin/categories/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateCategory_ReturnsNotFound_WhenNewParentDoesNotExist() throws Exception {
            Category category = saveCategory("Electronics", null);

            String body = """
                    {"parentId":"%s"}
                    """.formatted(UUID.randomUUID());

            mockMvc.perform(patch("/api/v1/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateCategory_ReturnsForbidden_WhenUserIsNotAdmin() throws Exception {
            Category category = saveCategory("Electronics", null);

            String body = """
                    {"name":"Consumer Electronics"}
                    """;

            mockMvc.perform(patch("/api/v1/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class DeleteCategory {
        @Test
        void deleteCategory_DeletesCategory_WhenAdminAndCategoryExists() throws Exception {
            Category category = saveCategory("Electronics", null);

            mockMvc.perform(delete("/api/v1/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Category deleted successfully"));

            mockMvc.perform(get("/api/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }

        @Test
        void deleteCategory_ReturnsNotFound_WhenCategoryDoesNotExist() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/categories/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteCategory_ReturnsForbidden_WhenUserIsNotAdmin() throws Exception {
            Category category = saveCategory("Electronics", null);

            mockMvc.perform(delete("/api/v1/admin/categories/{id}", category.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deleteCategory_ReturnsUnauthorized_WhenNoTokenProvided() throws Exception {
            Category category = saveCategory("Electronics", null);

            mockMvc.perform(delete("/api/v1/admin/categories/{id}", category.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}