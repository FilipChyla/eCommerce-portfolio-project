package io.github.filipchyla.shopapi.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.cart.CartLimitPolicy;
import io.github.filipchyla.shopapi.cart.domain.GuestCart;
import io.github.filipchyla.shopapi.cart.dto.AddCartItemRequest;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.cart.dto.UpdateCartItemRequest;
import io.github.filipchyla.shopapi.cart.exception.CartItemLimitExceededException;
import io.github.filipchyla.shopapi.cart.exception.CartItemNotFoundException;
import io.github.filipchyla.shopapi.cart.exception.InsufficientStockException;
import io.github.filipchyla.shopapi.cart.mapper.GuestCartMapper;
import io.github.filipchyla.shopapi.product.Product;
import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestCartServiceTest {

    @Mock
    private RedisTemplate<String, String> redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ProductService productService;

    @Mock
    private CartLimitPolicy cartLimitPolicy;

    @Mock
    private GuestCartMapper guestCartMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GuestCartService guestCartService;

    private String cartToken;
    private UUID productId;
    private Product product;
    private CartResponse cartResponse;

    @BeforeEach
    void setUp() {
        lenient().when(redis.opsForValue()).thenReturn(valueOperations);

        cartToken = UUID.randomUUID().toString();
        productId = UUID.randomUUID();

        product = new Product();
        product.setId(productId);
        product.setName("Mouse");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStockQuantity(10);

        cartResponse = mock(CartResponse.class);

        lenient().when(guestCartMapper.toCartResponse(any(), anyMap()))
                .thenReturn(cartResponse);
    }

    @Nested
    class GetCart {

        @Test
        void getCart_ShouldReturnEmptyCart_WhenTokenIsNull() {
            // When
            CartResponse result = guestCartService.getCart(null);

            // Then
            assertThat(result).isSameAs(cartResponse);
            verifyNoInteractions(redis);
            verify(guestCartMapper).toCartResponse(
                    any(GuestCart.class),
                    anyMap()
            );
        }

        @Test
        void getCart_ShouldReturnMappedCart_WhenCartExists() throws JsonProcessingException {
            // Given
            GuestCart cart = new GuestCart();

            when(valueOperations.get(anyString())).thenReturn("cart-json");
            when(objectMapper.readValue("cart-json", GuestCart.class))
                    .thenReturn(cart);

            // When
            CartResponse result = guestCartService.getCart(cartToken);

            // Then
            assertThat(result).isSameAs(cartResponse);

            verify(valueOperations).get("guest-cart:" + cartToken);
            verify(objectMapper).readValue("cart-json", GuestCart.class);
            verify(productService).findAllByIds(anyList());
            verify(guestCartMapper).toCartResponse(
                    same(cart),
                    anyMap()
            );
        }

        @Test
        void getCart_ShouldThrowIllegalStateException_WhenJsonIsCorrupted() throws JsonProcessingException {
            // Given
            when(valueOperations.get("guest-cart:" + cartToken)).thenReturn("invalid-json");
            when(objectMapper.readValue("invalid-json", GuestCart.class))
                    .thenThrow(new JsonProcessingException("corrupt") {});

            // When & Then
            assertThatThrownBy(() -> guestCartService.getCart(cartToken))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Corrupted guest cart data");
        }

        @Test
        void getCart_ShouldRemoveStaleProductsAndPersistCleanedCart_WhenProductsNoLongerExist() throws JsonProcessingException {
            // Given
            GuestCart cart = new GuestCart();
            cart.addOrIncreaseItem(productId, 2, BigDecimal.TEN);
            UUID staleId = UUID.randomUUID();
            cart.addOrIncreaseItem(staleId, 1, BigDecimal.ONE);

            when(valueOperations.get("guest-cart:" + cartToken)).thenReturn("cart-json");
            when(objectMapper.readValue("cart-json", GuestCart.class)).thenReturn(cart);
            when(productService.findAllByIds(anyList())).thenReturn(List.of(product));
            when(objectMapper.writeValueAsString(any(GuestCart.class))).thenReturn("cleaned-json");

            // When
            guestCartService.getCart(cartToken);

            // Then
            verify(valueOperations).set(eq("guest-cart:" + cartToken), eq("cleaned-json"), eq(Duration.ofDays(7)));
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().getFirst().getProductId()).isEqualTo(productId);
        }
    }

    @Nested
    class AddItem {

        @Test
        void addItem_ShouldStoreItem_WhenValid() throws JsonProcessingException {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);
            when(productService.findActiveProductById(productId))
                    .thenReturn(Optional.of(product));

            when(objectMapper.writeValueAsString(any(GuestCart.class)))
                    .thenReturn("cart-json");

            // When
            CartResponse result = guestCartService.addItem(
                    cartToken,
                    new AddCartItemRequest(productId, 2)
            );

            // Then
            assertThat(result).isSameAs(cartResponse);

            verify(productService).findActiveProductById(productId);

            verify(cartLimitPolicy).validateAddition(
                    same(product),
                    eq(2),
                    eq(true),
                    eq(0)
            );

            verify(valueOperations).set(
                    eq("guest-cart:" + cartToken),
                    eq("cart-json"),
                    eq(Duration.ofDays(7))
            );

            verify(guestCartMapper).toCartResponse(
                    any(GuestCart.class),
                    anyMap()
            );
        }

        @Test
        void addItem_ShouldCorrectlyAccumulateQuantity_WhenProductAlreadyExistsInCart() throws JsonProcessingException {
            // Given
            GuestCart cart = new GuestCart();
            cart.addOrIncreaseItem(productId, 3, BigDecimal.valueOf(99.99));

            when(valueOperations.get("guest-cart:" + cartToken)).thenReturn("cart-json");
            when(objectMapper.readValue("cart-json", GuestCart.class)).thenReturn(cart);
            when(productService.findActiveProductById(productId)).thenReturn(Optional.of(product));
            when(productService.findAllByIds(anyList())).thenReturn(List.of(product));
            when(objectMapper.writeValueAsString(any(GuestCart.class))).thenReturn("updated-json");

            // When
            guestCartService.addItem(cartToken, new AddCartItemRequest(productId, 2));

            // Then
            verify(cartLimitPolicy).validateAddition(same(product), eq(5), eq(false), eq(1));
            assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(5);
            verify(valueOperations).set(eq("guest-cart:" + cartToken), eq("updated-json"), eq(Duration.ofDays(7)));
        }

        @Test
        void addItem_ShouldThrowProductNotFound_WhenProductDoesNotExistOrInactive() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);
            when(productService.findActiveProductById(productId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> guestCartService.addItem(cartToken, new AddCartItemRequest(productId, 1)))
                    .isInstanceOf(ProductNotFoundException.class);

            verifyNoInteractions(cartLimitPolicy);
            verify(valueOperations, never()).set(anyString(), anyString(), any());
        }

        @Test
        void addItem_ShouldThrowInsufficientStock_WhenRequestExceedsStock() {
            // Given
            product.setStockQuantity(1);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(productService.findActiveProductById(productId))
                    .thenReturn(Optional.of(product));
            doThrow(InsufficientStockException.class)
                    .when(cartLimitPolicy).validateAddition(eq(product), eq(5), anyBoolean(), anyInt());

            // When & Then
            assertThatThrownBy(() ->
                    guestCartService.addItem(
                            cartToken,
                            new AddCartItemRequest(productId, 5)
                    )
            ).isInstanceOf(InsufficientStockException.class);

            verify(valueOperations, never())
                    .set(anyString(), anyString(), any(Duration.class));

            verifyNoInteractions(guestCartMapper);
        }

        @Test
        void addItem_ShouldThrowLimitExceeded_WhenQuantityAboveMax() {
            // Given
            var request = new AddCartItemRequest(productId, 6);

            when(valueOperations.get(anyString())).thenReturn(null);
            when(productService.findActiveProductById(productId))
                    .thenReturn(Optional.of(product));

            doThrow(CartItemLimitExceededException.class)
                    .when(cartLimitPolicy)
                    .validateAddition(
                            eq(product),
                            eq(request.quantity()),
                            anyBoolean(),
                            anyInt()
                    );

            // When & Then
            assertThatThrownBy(() ->
                    guestCartService.addItem(cartToken, request)
            ).isInstanceOf(CartItemLimitExceededException.class);

            verify(valueOperations, never())
                    .set(anyString(), anyString(), any(Duration.class));

            verifyNoInteractions(guestCartMapper);
        }
    }

    @Nested
    class UpdateAndRemove {

        @Test
        void updateItemQuantity_ShouldUpdateQuantity_WhenItemExists() throws JsonProcessingException {
            // Given
            GuestCart cart = new GuestCart();
            cart.addOrIncreaseItem(productId, 2, BigDecimal.valueOf(99.99));

            when(valueOperations.get("guest-cart:" + cartToken)).thenReturn("cart-json");
            when(objectMapper.readValue("cart-json", GuestCart.class)).thenReturn(cart);
            when(productService.findProductById(productId)).thenReturn(Optional.of(product));
            when(productService.findAllByIds(anyList())).thenReturn(List.of(product));
            when(objectMapper.writeValueAsString(any(GuestCart.class))).thenReturn("updated-json");

            // When
            guestCartService.updateItemQuantity(cartToken, productId, new UpdateCartItemRequest(4));

            // Then
            verify(cartLimitPolicy).validateQuantityUpdate(product, 4);
            assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(4);
            verify(valueOperations).set(eq("guest-cart:" + cartToken), eq("updated-json"), eq(Duration.ofDays(7)));
        }

        @Test
        void updateItemQuantity_ShouldThrowInsufficientStock_WhenNewQuantityExceedsStock() throws JsonProcessingException {
            // Given
            GuestCart cart = new GuestCart();
            cart.addOrIncreaseItem(productId, 2, BigDecimal.valueOf(99.99));

            when(valueOperations.get("guest-cart:" + cartToken)).thenReturn("cart-json");
            when(objectMapper.readValue("cart-json", GuestCart.class)).thenReturn(cart);
            when(productService.findProductById(productId)).thenReturn(Optional.of(product));
            doThrow(InsufficientStockException.class).when(cartLimitPolicy).validateQuantityUpdate(product, 15);

            // When & Then
            assertThatThrownBy(() -> guestCartService.updateItemQuantity(cartToken, productId, new UpdateCartItemRequest(15)))
                    .isInstanceOf(InsufficientStockException.class);

            verify(valueOperations, never()).set(anyString(), anyString(), any());
        }

        @Test
        void updateItemQuantity_ShouldThrowProductNotFound_WhenProductDoesNotExist() throws JsonProcessingException {
            // Given
            GuestCart cart = new GuestCart();
            cart.addOrIncreaseItem(productId, 2, BigDecimal.valueOf(99.99));

            when(valueOperations.get("guest-cart:" + cartToken)).thenReturn("cart-json");
            when(objectMapper.readValue("cart-json", GuestCart.class)).thenReturn(cart);
            when(productService.findProductById(productId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> guestCartService.updateItemQuantity(cartToken, productId, new UpdateCartItemRequest(4)))
                    .isInstanceOf(ProductNotFoundException.class);
        }

        @Test
        void updateItemQuantity_ShouldThrowNotFound_WhenItemMissing() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    guestCartService.updateItemQuantity(
                            cartToken,
                            productId,
                            new UpdateCartItemRequest(3)
                    )
            ).isInstanceOf(CartItemNotFoundException.class);

            verifyNoInteractions(productService);
            verifyNoInteractions(cartLimitPolicy);
            verifyNoInteractions(guestCartMapper);
        }

        @Test
        void removeItem_ShouldRemoveItemAndSave_WhenItemExists() throws JsonProcessingException {
            // Given
            GuestCart cart = new GuestCart();
            cart.addOrIncreaseItem(productId, 2, BigDecimal.valueOf(99.99));

            when(valueOperations.get("guest-cart:" + cartToken)).thenReturn("cart-json");
            when(objectMapper.readValue("cart-json", GuestCart.class)).thenReturn(cart);
            when(productService.findAllByIds(anyList())).thenReturn(List.of());
            when(objectMapper.writeValueAsString(any(GuestCart.class))).thenReturn("empty-cart-json");

            // When
            guestCartService.removeItem(cartToken, productId);

            // Then
            assertThat(cart.getItems()).isEmpty();
            verify(valueOperations).set(eq("guest-cart:" + cartToken), eq("empty-cart-json"), eq(Duration.ofDays(7)));
        }

        @Test
        void removeItem_ShouldThrowNotFound_WhenItemMissing() {
            // Given
            when(valueOperations.get(anyString())).thenReturn(null);

            // When & Then
            assertThatThrownBy(() ->
                    guestCartService.removeItem(cartToken, productId)
            ).isInstanceOf(CartItemNotFoundException.class);

            verifyNoInteractions(productService);
            verifyNoInteractions(guestCartMapper);
        }
    }

    @Nested
    class ClearCart {

        @Test
        void clearCart_ShouldDeleteKey_WhenTokenPresent() {
            // When
            guestCartService.clearCart(cartToken);

            // Then
            verify(redis).delete("guest-cart:" + cartToken);
        }

        @Test
        void clearCart_ShouldDoNothing_WhenTokenIsNull() {
            // When
            guestCartService.clearCart(null);

            // Then
            verifyNoInteractions(redis);
        }
    }
}