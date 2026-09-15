package io.github.filipchyla.shopapi.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.cart.dto.*;
import io.github.filipchyla.shopapi.cart.service.CartMergeService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.security.filter.JwtAuthenticationFilter;
import io.github.filipchyla.shopapi.user.User;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtAuthenticationFilter jwtFilter;
    @MockitoBean
    private CartFacade cartFacade;
    @MockitoBean
    private CartMergeService cartMergeService;
    @MockitoBean
    private CartTokenCookieFactory cartTokenCookieFactory;

    private static final String BASE_URL = "/api/v1/cart";
    private User user;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@example.com");
        UserPrincipal userPrincipal = new UserPrincipal(user);
        auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, List.of());

        lenient().when(cartTokenCookieFactory.create(anyString()))
                .thenAnswer(inv -> ResponseCookie.from("cart_token", inv.getArgument(0)).build());
        lenient().when(cartTokenCookieFactory.clear())
                .thenReturn(ResponseCookie.from("cart_token", "").maxAge(0).build());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class GetCart {
        @Test
        void getCart_ShouldReturnCart_ForGuestWithNoCookie() throws Exception {
            // Given
            CartResponse response = new CartResponse(null, List.of(), BigDecimal.ZERO);
            when(cartFacade.getCart(isNull(), isNull())).thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(0)))
                    .andExpect(jsonPath("$.totalPrice").value(0));
        }

        @Test
        void getCart_ShouldReturnCart_ForGuestWithExistingCookie() throws Exception {
            // Given
            String token = "guest-cart-token-123";
            CartResponse response = new CartResponse(null, List.of(), BigDecimal.ZERO);
            when(cartFacade.getCart(isNull(), eq(token))).thenReturn(response);

            // When & Then
            mockMvc.perform(get(BASE_URL)
                            .cookie(new Cookie("cart_token", token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items", hasSize(0)));
        }
    }

    @Nested
    class AddItem {
        @Test
        void addItem_ShouldSetCartTokenCookie_WhenGuestHasNoCookieYet() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            CartResponse response = new CartResponse(null, List.of(), BigDecimal.TEN);
            String newToken = UUID.randomUUID().toString();

            when(cartFacade.addItem(isNull(), isNull(), any(AddCartItemRequest.class)))
                    .thenReturn(new CartActionResult(response, newToken));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AddCartItemRequest(productId, 2))))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().string("Set-Cookie", containsString("cart_token=" + newToken)));
        }

        @Test
        void addItem_ShouldNotSetCookie_WhenGuestAlreadyHasCartToken() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            String existingToken = "existing-token";
            CartResponse response = new CartResponse(null, List.of(), BigDecimal.TEN);

            when(cartFacade.addItem(isNull(), eq(existingToken), any(AddCartItemRequest.class)))
                    .thenReturn(new CartActionResult(response, null));

            // When & Then
            mockMvc.perform(post(BASE_URL + "/items")
                            .cookie(new Cookie("cart_token", existingToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new AddCartItemRequest(productId, 2))))
                    .andExpect(status().isOk())
                    .andExpect(header().doesNotExist("Set-Cookie"));
        }

        @Test
        void addItem_ShouldReturnBadRequest_WhenQuantityIsZero() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            String body = """
                    {"productId":"%s","quantity":0}
                    """.formatted(productId);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addItem_ShouldReturnBadRequest_WhenQuantityExceedsMax() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            String body = """
                    {"productId":"%s","quantity":21}
                    """.formatted(productId);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addItem_ShouldReturnBadRequest_WhenProductIdIsNull() throws Exception {
            // Given
            String body = """
                    {"quantity":2}
                    """;

            // When & Then
            mockMvc.perform(post(BASE_URL + "/items")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class UpdateItemQuantity {

        @Test
        void updateItemQuantity_ShouldReturnOk_WhenValidQuantityProvided() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            CartResponse response = new CartResponse(null, List.of(), BigDecimal.TEN);
            when(cartFacade.updateItemQuantity(isNull(), isNull(), eq(productId), any(UpdateCartItemRequest.class)))
                    .thenReturn(response);

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/items/{productId}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateCartItemRequest(5))))
                    .andExpect(status().isOk());
        }

        @Test
        void updateItemQuantity_ShouldReturnBadRequest_WhenQuantityIsZeroOrNegative() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            String body = """
                    {"quantity":0}
                    """;

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/items/{productId}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateItemQuantity_ShouldReturnBadRequest_WhenQuantityExceedsMaxAllowed() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            String body = """
                    {"quantity":21}
                    """;

            // When & Then
            mockMvc.perform(patch(BASE_URL + "/items/{productId}", productId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class RemoveItem {

        @Test
        void removeItem_ShouldReturnOk_WhenProductRemoved() throws Exception {
            // Given
            UUID productId = UUID.randomUUID();
            CartResponse response = new CartResponse(null, List.of(), BigDecimal.ZERO);
            when(cartFacade.removeItem(isNull(), isNull(), eq(productId))).thenReturn(response);

            // When & Then
            mockMvc.perform(delete(BASE_URL + "/items/{productId}", productId))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ClearCart {

        @Test
        void clearCart_ShouldReturnNoContentAndClearCookie() throws Exception {
            // When & Then
            mockMvc.perform(delete(BASE_URL))
                    .andExpect(status().isNoContent())
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

            verify(cartFacade).clearCart(isNull(), isNull());
        }
    }

    @Nested
    class MergeCart {

        @Test
        void mergeCart_ShouldReturnOkAndClearCookie_WhenAuthenticated() throws Exception {
            // Given
            SecurityContextHolder.getContext().setAuthentication(auth);
            String token = "guest-token";
            CartMergeResponse mergeResponse = new CartMergeResponse(
                    new CartResponse(UUID.randomUUID(), List.of(), BigDecimal.ZERO),
                    List.of()
            );

            when(cartMergeService.merge(eq(token), eq(user))).thenReturn(mergeResponse);

            // When & Then
            mockMvc.perform(post(BASE_URL + "/merge")
                            .cookie(new Cookie("cart_token", token)))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Set-Cookie"))
                    .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")))
                    .andExpect(jsonPath("$.skippedItems", hasSize(0)));
        }
    }
}
