package io.github.filipchyla.shopapi.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.cart.service.CartService;
import io.github.filipchyla.shopapi.product.Product;
import io.github.filipchyla.shopapi.product.ProductRepository;
import io.github.filipchyla.shopapi.product.category.Category;
import io.github.filipchyla.shopapi.product.category.CategoryRepository;
import io.github.filipchyla.shopapi.role.Role;
import io.github.filipchyla.shopapi.role.RoleName;
import io.github.filipchyla.shopapi.role.RoleRepository;
import io.github.filipchyla.shopapi.user.User;
import io.github.filipchyla.shopapi.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles({"test", "testcontainers"})
@AutoConfigureMockMvc
class CartIntegrationTest {

    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CartLimitPolicy cartLimitPolicy;
    @Autowired private CartService cartService;
    @Autowired private CartFacade cartFacade;
    @Autowired private CartTokenCookieFactory cartTokenCookieFactory;

    private String userToken;
    private Product product;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shopdb").withUsername("test").withPassword("test");

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
        cartRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        Role userRole = roleRepository.findByName(RoleName.USER).orElseThrow();
        User user = new User();
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("UserPassword123!"));
        user.setFirstName("Plain");
        user.setLastName("User");
        user.setEnabled(true);
        user.setRole(userRole);
        userRepository.save(user);

        userToken = obtainToken("user@example.com", "UserPassword123!");

        Category category = new Category();
        category.setName("Electronics");
        categoryRepository.save(category);

        product = new Product();
        product.setName("Mouse");
        product.setDescription("A mouse");
        product.setPrice(new BigDecimal("49.99"));
        product.setStockQuantity(5);
        product.setCategory(category);
        product = productRepository.save(product);
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

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Nested
    class GuestFlow {
        @Test
        void addItem_CreatesCookieAndAddsItem_WhenNoCookiePresent() throws Exception {
            String body = """
                    {"productId":"%s","quantity":2}
                    """.formatted(product.getId());

            mockMvc.perform(post("/api/v1/cart/items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(cookie().exists("cart_token"))
                    .andExpect(jsonPath("$.items", hasSize(1)))
                    .andExpect(jsonPath("$.items[0].quantity").value(2));
        }

        @Test
        void addItem_ReturnsConflict_WhenStockInsufficient() throws Exception {
            String body = """
                    {"productId":"%s","quantity":10}
                    """.formatted(product.getId());

            mockMvc.perform(post("/api/v1/cart/items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        void getCart_ReturnsPreviouslyAddedItems_WhenCookieReused() throws Exception {
            String addBody = """
                    {"productId":"%s","quantity":1}
                    """.formatted(product.getId());

            MvcResult addResult = mockMvc.perform(post("/api/v1/cart/items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addBody))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie cartCookie = addResult.getResponse().getCookie("cart_token");

            mockMvc.perform(get("/api/v1/cart")
                            .cookie(cartCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)));
        }
    }

    @Nested
    class UserFlow {
        @Test
        @Transactional
        void addItem_PersistsToDatabase_WhenAuthenticated() throws Exception {
            String body = """
                    {"productId":"%s","quantity":2}
                    """.formatted(product.getId());

            mockMvc.perform(post("/api/v1/cart/items")
                            .with(csrf())
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(1)));

            assertThat(cartRepository.findAll()).hasSize(1);
            assertThat(cartRepository.findAll().getFirst().getItems()).hasSize(1);
        }
    }

    @Nested
    class MergeFlow {
        @Test
        void mergeCart_MovesGuestItemsToUserCart_AndClearsCookie() throws Exception {
            String addBody = """
                    {"productId":"%s","quantity":2}
                    """.formatted(product.getId());

            MvcResult addResult = mockMvc.perform(post("/api/v1/cart/items")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(addBody))
                    .andExpect(status().isOk())
                    .andReturn();

            Cookie cartCookie = addResult.getResponse().getCookie("cart_token");

            mockMvc.perform(post("/api/v1/cart/merge")
                            .with(csrf())
                            .header("Authorization", "Bearer " + userToken)
                            .cookie(cartCookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cart.items", hasSize(1)))
                    .andExpect(jsonPath("$.skippedItems", hasSize(0)));
        }

        @Test
        void mergeCart_ReturnsUnauthorized_WhenNoToken() throws Exception {
            mockMvc.perform(post("/api/v1/cart/merge")
                    .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }
}
