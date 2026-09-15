package io.github.filipchyla.shopapi.config.authorization;

import io.github.filipchyla.shopapi.cart.CartController;
import io.github.filipchyla.shopapi.cart.CartFacade;
import io.github.filipchyla.shopapi.cart.CartTokenCookieFactory;
import io.github.filipchyla.shopapi.cart.service.CartMergeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc
class CartAuthorizationTest extends AuthorizationTest {
    @MockitoBean
    private CartFacade cartFacade;
    @MockitoBean
    private CartMergeService cartMergeService;
    @MockitoBean
    private CartTokenCookieFactory cartTokenCookieFactory;

    @Test
    void getCart_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().is(not(401)));
    }

    @Test
    void addItem_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/cart/items")
                        .with(csrf()))
                .andExpect(status().is(not(401)));
    }

    @Test
    void updateItemQuantity_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(patch("/api/v1/cart/items/{productId}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().is(not(401)));
    }

    @Test
    void removeItem_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/items/{productId}", UUID.randomUUID())
                        .with(csrf()))
                .andExpect(status().is(not(401)));
    }

    @Test
    void clearCart_IsAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(delete("/api/v1/cart")
                        .with(csrf()))
                .andExpect(status().is(not(401)));
    }

    @Test
    void mergeCart_IsNotAccessible_WithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/cart/merge")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mergeCart_IsAccessible_WhenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/cart/merge")
                        .with(user("testuser").roles("USER"))
                        .with(csrf()))
                .andExpect(status().is(not(403)))
                .andExpect(status().is(not(401)));
    }
}
