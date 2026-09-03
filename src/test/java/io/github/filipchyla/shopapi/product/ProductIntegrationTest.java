package io.github.filipchyla.shopapi.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryRepository;
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

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles({"test", "testcontainers"})
@AutoConfigureMockMvc
class ProductIntegrationTest {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
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
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
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

    private Category saveCategory(String name) {
        Category category = new Category();
        category.setName(name);
        return categoryRepository.save(category);
    }

    private Product saveProduct(String name, BigDecimal price, int stockQuantity, Category category) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " description");
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        product.setCategory(category);
        return productRepository.save(product);
    }


    @Nested
    class GetProducts {
        @Test
        void getProducts_ReturnsEmptyPage_WhenNoProductsExist() throws Exception {
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }

        @Test
        void getProducts_ReturnsAllProducts_WhenNoFiltersApplied() throws Exception {
            Category category = saveCategory("Electronics");
            saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);
            saveProduct("Mouse", new BigDecimal("99.99"), 20, category);

            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)));
        }

        @Test
        void getProducts_FiltersByCategoryId() throws Exception {
            Category electronics = saveCategory("Electronics");
            Category books = saveCategory("Books");
            saveProduct("Laptop", new BigDecimal("2999.99"), 5, electronics);
            saveProduct("Novel", new BigDecimal("29.99"), 10, books);

            mockMvc.perform(get("/api/v1/products").param("categoryId", electronics.getId().toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Laptop"));
        }

        @Test
        void getProducts_FiltersByPriceRange() throws Exception {
            Category category = saveCategory("Electronics");
            saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);
            saveProduct("Mouse", new BigDecimal("99.99"), 20, category);

            mockMvc.perform(get("/api/v1/products")
                            .param("minPrice", "50")
                            .param("maxPrice", "200"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name").value("Mouse"));
        }

        @Test
        void getProducts_ReturnsBadRequest_WhenSortFieldIsNotAllowed() throws Exception {
            mockMvc.perform(get("/api/v1/products").param("sort", "stockQuantity,asc"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void getProducts_DoesNotRequireAuthentication() throws Exception {
            mockMvc.perform(get("/api/v1/products"))
                    .andExpect(status().isOk());
        }
    }


    @Nested
    class GetProduct {
        @Test
        void getProduct_ReturnsProduct_WhenProductExists() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.description").value("Laptop description"))
                    .andExpect(jsonPath("$.price").value(2999.99))
                    .andExpect(jsonPath("$.stockQuantity").value(5))
                    .andExpect(jsonPath("$.categoryName").value(category.getName()));
        }

        @Test
        void getProduct_ReturnsNotFound_WhenProductDoesNotExist() throws Exception {
            mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getProduct_ReflectsUpdatedStock_AfterCachePutOnStockUpdate() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stockQuantity").value(5));

            String stockBody = """
                    {"difference":42}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}/stock", product.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(stockBody))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stockQuantity").value(47));
        }
    }


    @Nested
    class AddProduct {
        @Test
        void addProduct_CreatesProduct_WhenAdminAndValidRequest() throws Exception {
            Category category = saveCategory("Electronics");

            String body = """
                    {"name":"Laptop","description":"A laptop","price":2999.99,"stockQuantity":5,"categoryId":"%s"}
                    """.formatted(category.getId());

            mockMvc.perform(post("/api/v1/admin/products")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.description").value("A laptop"))
                    .andExpect(jsonPath("$.price").value(2999.99))
                    .andExpect(jsonPath("$.stockQuantity").value(5))
                    .andExpect(jsonPath("$.categoryName").value(category.getName()));
        }

        @Test
        void addProduct_ReturnsBadRequest_WhenNameIsBlank() throws Exception {
            Category category = saveCategory("Electronics");

            String body = """
                    {"name":"","description":"A laptop","price":2999.99,"stockQuantity":5,"categoryId":"%s"}
                    """.formatted(category.getId());

            mockMvc.perform(post("/api/v1/admin/products")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addProduct_ReturnsNotFound_WhenCategoryIdDoesNotExist() throws Exception {
            String body = """
                    {"name":"Laptop","description":"A laptop","price":2999.99,"stockQuantity":5,"categoryId":"%s"}
                    """.formatted(UUID.randomUUID());

            mockMvc.perform(post("/api/v1/admin/products")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        void addProduct_ReturnsForbidden_WhenUserIsNotAdmin() throws Exception {
            Category category = saveCategory("Electronics");

            String body = """
                    {"name":"Laptop","description":"A laptop","price":2999.99,"stockQuantity":5,"categoryId":"%s"}
                    """.formatted(category.getId());

            mockMvc.perform(post("/api/v1/admin/products")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        void addProduct_ReturnsUnauthorized_WhenNoTokenProvided() throws Exception {
            Category category = saveCategory("Electronics");

            String body = """
                    {"name":"Laptop","description":"A laptop","price":2999.99,"stockQuantity":5,"categoryId":"%s"}
                    """.formatted(category.getId());

            mockMvc.perform(post("/api/v1/admin/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }


    @Nested
    class UpdateStock {
        @Test
        void updateStock_UpdatesQuantity_WhenAdminAndValidRequest() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            String body = """
                    {"difference":10}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}/stock", product.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stockQuantity").value(15));
        }

        @Test
        void updateStock_DecreasesQuantity_WhenAdminAndValidRequest() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            String body = """
                    {"difference":-3}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}/stock", product.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stockQuantity").value(2));
        }

        @Test
        void updateStock_ReturnsNotFound_WhenProductDoesNotExist() throws Exception {
            String body = """
                    {"difference":15}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}/stock", UUID.randomUUID())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateStock_ReturnsForbidden_WhenUserIsNotAdmin() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            String body = """
                    {"quantity":15}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}/stock", product.getId())
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }


    @Nested
    class UpdateProduct {
        @Test
        void updateProduct_UpdatesFields_WhenAdminAndValidRequest() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            String body = """
                    {"name":"Gaming Laptop","price":3499.99}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}", product.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Gaming Laptop"))
                    .andExpect(jsonPath("$.price").value(3499.99));
        }

        @Test
        void updateProduct_ReassignsCategory_WhenCategoryIdProvided() throws Exception {
            Category oldCategory = saveCategory("Electronics");
            Category newCategory = saveCategory("Computers");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, oldCategory);

            String body = """
                    {"categoryId":"%s"}
                    """.formatted(newCategory.getId());

            mockMvc.perform(patch("/api/v1/admin/products/{id}", product.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.categoryName").value(newCategory.getName()));
        }

        @Test
        void updateProduct_ReturnsBadRequest_WhenPriceIsNegative() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            String body = """
                    {"price":-10}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}", product.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateProduct_ReturnsNotFound_WhenProductDoesNotExist() throws Exception {
            String body = """
                    {"name":"Doesn't matter"}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateProduct_ReturnsNotFound_WhenNewCategoryDoesNotExist() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            String body = """
                    {"categoryId":"%s"}
                    """.formatted(UUID.randomUUID());

            mockMvc.perform(patch("/api/v1/admin/products/{id}", product.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateProduct_ReturnsForbidden_WhenUserIsNotAdmin() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            String body = """
                    {"name":"Gaming Laptop"}
                    """;

            mockMvc.perform(patch("/api/v1/admin/products/{id}", product.getId())
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }
    }


    @Nested
    class DeleteProduct {
        @Test
        void deleteProduct_DeletesProduct_WhenAdminAndProductExists() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            mockMvc.perform(delete("/api/v1/admin/products/{id}", product.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Product deleted successfully"));

            mockMvc.perform(get("/api/v1/products/{id}", product.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteProduct_ReturnsNotFound_WhenProductDoesNotExist() throws Exception {
            mockMvc.perform(delete("/api/v1/admin/products/{id}", UUID.randomUUID())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteProduct_ReturnsForbidden_WhenUserIsNotAdmin() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            mockMvc.perform(delete("/api/v1/admin/products/{id}", product.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        void deleteProduct_ReturnsUnauthorized_WhenNoTokenProvided() throws Exception {
            Category category = saveCategory("Electronics");
            Product product = saveProduct("Laptop", new BigDecimal("2999.99"), 5, category);

            mockMvc.perform(delete("/api/v1/admin/products/{id}", product.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }
}