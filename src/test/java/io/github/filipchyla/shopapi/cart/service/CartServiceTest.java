package io.github.filipchyla.shopapi.cart.service;

import io.github.filipchyla.shopapi.cart.CartLimitPolicy;
import io.github.filipchyla.shopapi.cart.CartRepository;
import io.github.filipchyla.shopapi.cart.domain.Cart;
import io.github.filipchyla.shopapi.cart.dto.AddCartItemRequest;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.cart.dto.UpdateCartItemRequest;
import io.github.filipchyla.shopapi.cart.exception.CartItemLimitExceededException;
import io.github.filipchyla.shopapi.cart.exception.CartItemNotFoundException;
import io.github.filipchyla.shopapi.cart.exception.InsufficientStockException;
import io.github.filipchyla.shopapi.cart.mapper.CartMapper;
import io.github.filipchyla.shopapi.product.Product;
import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import io.github.filipchyla.shopapi.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private ProductService productService;
    @Mock
    private CartMapper cartMapper;
    @Mock
    private CartLimitPolicy cartLimitPolicy;

    @InjectMocks
    private CartService cartService;

    private User user;
    private UUID userId;
    private UUID productId;
    private Product product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);

        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setName("Mouse");
        product.setPrice(BigDecimal.valueOf(99.99));
        product.setStockQuantity(10);


        lenient().when(cartMapper.toCartResponse(any(Cart.class)))
                .thenAnswer(invocation -> new CartResponse(((Cart) invocation.getArgument(0)).getId(), null, null));
    }

    private Cart existingCart() {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUser(user);
        return cart;
    }

    @Nested
    class GetCart {
        @Test
        void getCart_ShouldCreateNewCart_WhenNoneExistsForUser() {
            // Given
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
            when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            cartService.getCart(user);

            // Then
            verify(cartRepository).save(any(Cart.class));
        }

        @Test
        void getCart_ShouldReturnExistingCart_WhenOneExistsForUser() {
            // Given
            Cart cart = existingCart();
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            // When
            cartService.getCart(user);

            // Then
            verify(cartRepository, never()).save(any());
        }
    }

    @Nested
    class AddItem {
        @Test
        void addItem_ShouldAddNewLineItem_WhenProductNotYetInCart() {
            // Given
            Cart cart = existingCart();
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.ofNullable(product));

            // When
            cartService.addItem(user, new AddCartItemRequest(productId, 2));

            // Then
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(2);
            assertThat(cart.getItems().getFirst().getUnitPriceSnapshot()).isEqualByComparingTo("99.99");
        }

        @Test
        void addItem_ShouldIncreaseQuantity_WhenProductAlreadyInCart() {
            // Given
            Cart cart = existingCart();
            cart.addNewItem(product, 3);

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.ofNullable(product));

            // When
            cartService.addItem(user, new AddCartItemRequest(productId, 2));

            // Then
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(5);
        }

        @Test
        void addItem_ShouldThrowLimitExceeded_WhenTotalQuantityExceedsMax() {
            // Given
            Cart cart = existingCart();
            AddCartItemRequest request = new AddCartItemRequest(productId, 16);

            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.ofNullable(product));
            doThrow(CartItemLimitExceededException.class).when(cartLimitPolicy)
                    .validateAddition(
                            eq(product),
                            eq(request.quantity()),
                            anyBoolean(),
                            anyInt());

            // When & Then
            assertThatThrownBy(() -> cartService.addItem(user, request))
                    .isInstanceOf(CartItemLimitExceededException.class);
        }

        @Test
        void addItem_ShouldThrowInsufficientStock_WhenRequestedQuantityExceedsStock() {
            // Given
            product.setStockQuantity(1);
            Cart cart = existingCart();
            AddCartItemRequest request = new AddCartItemRequest(productId, 2);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.ofNullable(product));
            doThrow(InsufficientStockException.class).when(cartLimitPolicy)
                    .validateAddition(
                            eq(product),
                            eq(request.quantity()),
                            anyBoolean(),
                            anyInt());

            // When & Then
            assertThatThrownBy(() -> cartService.addItem(user, request))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        void addItem_ShouldThrowLimitExceeded_WhenCartAlreadyHasMaxDistinctItems() {
            // Given
            Cart cart = existingCart();
            AddCartItemRequest request = new AddCartItemRequest(productId, 1);
            for (int i = 0; i < 50; i++) {
                Product otherProduct = new Product();
                otherProduct.setId(UUID.randomUUID());
                otherProduct.setPrice(BigDecimal.TEN);
                cart.addNewItem(otherProduct, 1);
            }
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.ofNullable(product));
            doThrow(CartItemLimitExceededException.class).when(cartLimitPolicy)
                    .validateAddition(
                            eq(product),
                            eq(request.quantity()),
                            anyBoolean(),
                            anyInt());

            // When & Then
            assertThatThrownBy(() -> cartService.addItem(user, request))
                    .isInstanceOf(CartItemLimitExceededException.class);
        }
        @Test
        void addItem_ShouldThrowProductNotFound_WhenProductIsInactiveOrNotFound() {
            // Given
            Cart cart = existingCart();
            AddCartItemRequest request = new AddCartItemRequest(productId, 1);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> cartService.addItem(user, request))
                    .isInstanceOf(ProductNotFoundException.class);
            verifyNoInteractions(cartLimitPolicy);
        }

        @Test
        void addItem_ShouldPassIsNewTrueToPolicy_WhenAddingNewItem() {
            // Given
            Cart cart = existingCart();
            AddCartItemRequest request = new AddCartItemRequest(productId, 2);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.ofNullable(product));

            // When
            cartService.addItem(user, request);

            // Then
            verify(cartLimitPolicy).validateAddition(eq(product), eq(2), eq(true), eq(0));
        }

        @Test
        void addItem_ShouldPassIsNewFalseToPolicy_WhenAddingExistingItem() {
            // Given
            Cart cart = existingCart();
            cart.addNewItem(product, 3);
            AddCartItemRequest request = new AddCartItemRequest(productId, 2);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            when(productService.findActiveProductById(productId)).thenReturn(Optional.ofNullable(product));

            // When
            cartService.addItem(user, request);

            // Then
            verify(cartLimitPolicy).validateAddition(eq(product), eq(5), eq(false), eq(1));
        }
    }

    @Nested
    class UpdateItemQuantity {
        @Test
        void updateItemQuantity_ShouldUpdateQuantity_WhenItemExistsAndStockSufficient() {
            // Given
            Cart cart = existingCart();
            cart.addNewItem(product, 2);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            // When
            cartService.updateItemQuantity(user, productId, new UpdateCartItemRequest(5));

            // Then
            assertThat(cart.findItemByProductId(productId).getQuantity()).isEqualTo(5);
            verify(cartLimitPolicy).validateQuantityUpdate(product, 5);
        }

        @Test
        void updateItemQuantity_ShouldThrowInsufficientStock_WhenPolicyRejectsUpdate() {
            // Given
            Cart cart = existingCart();
            cart.addNewItem(product, 2);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            doThrow(InsufficientStockException.class).when(cartLimitPolicy).validateQuantityUpdate(product, 15);

            // When & Then
            assertThatThrownBy(() -> cartService.updateItemQuantity(user, productId, new UpdateCartItemRequest(15)))
                    .isInstanceOf(InsufficientStockException.class);
        }

        @Test
        void updateItemQuantity_ShouldThrowLimitExceeded_WhenQuantityExceedsMaxItemLimit() {
            // Given
            Cart cart = existingCart();
            cart.addNewItem(product, 2);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
            doThrow(CartItemLimitExceededException.class).when(cartLimitPolicy).validateQuantityUpdate(product, 25);

            // When & Then
            assertThatThrownBy(() -> cartService.updateItemQuantity(user, productId, new UpdateCartItemRequest(25)))
                    .isInstanceOf(CartItemLimitExceededException.class);
        }

        @Test
        void updateItemQuantity_ShouldThrowNotFound_WhenItemNotInCart() {
            // Given
            Cart cart = existingCart();
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            // When & Then
            assertThatThrownBy(() -> cartService.updateItemQuantity(user, productId, new UpdateCartItemRequest(5)))
                    .isInstanceOf(CartItemNotFoundException.class);
        }
    }

    @Nested
    class RemoveItem {
        @Test
        void removeItem_ShouldRemoveItem_WhenItExists() {
            // Given
            Cart cart = existingCart();
            cart.addNewItem(product, 1);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            // When
            cartService.removeItem(user, productId);

            // Then
            assertThat(cart.getItems()).isEmpty();
        }

        @Test
        void removeItem_ShouldThrowNotFound_WhenItemDoesNotExist() {
            // Given
            Cart cart = existingCart();
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            // When & Then
            assertThatThrownBy(() -> cartService.removeItem(user, productId))
                    .isInstanceOf(CartItemNotFoundException.class);
        }
    }

    @Nested
    class ClearCart {
        @Test
        void clearCart_ShouldClearAllItems_WhenCartExists() {
            // Given
            Cart cart = existingCart();
            cart.addNewItem(product, 3);
            when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

            // When
            cartService.clearCart(user);

            // Then
            assertThat(cart.getItems()).isEmpty();
        }
    }
}
