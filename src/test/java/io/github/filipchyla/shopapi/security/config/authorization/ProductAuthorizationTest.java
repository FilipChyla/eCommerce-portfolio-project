package io.github.filipchyla.shopapi.security.config.authorization;

import io.github.filipchyla.shopapi.product.ProductController;
import io.github.filipchyla.shopapi.product.ProductMapper;
import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc
class ProductAuthorizationTest extends AuthorizationTest {
    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private ProductMapper productMapper;

    @Test
    void getProducts_IsAccessible_WithoutAuthentication() throws Exception {
        Page<ProductResponse> products = new PageImpl<>(List.of());

        when(productService.findProducts(
                        nullable(UUID.class),
                        nullable(BigDecimal.class),
                        nullable(BigDecimal.class),
                        any(Pageable.class))
        ).thenReturn(products);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk());
    }

    @Test
    void getProductById_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/products/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    void updateStock_IsNotAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/products/{id}/stock", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateStock_ReturnsForbidden_WhenUser() throws Exception {
        mockMvc.perform(patch("/api/v1/products/{id}/stock", UUID.randomUUID())
                        .with(user("testuser").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStock_IsAccessible_WhenAdmin() throws Exception {
        mockMvc.perform(patch("/api/v1/products/{id}/stock", UUID.randomUUID())
                        .with(user("testadmin").roles("ADMIN")))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }
}