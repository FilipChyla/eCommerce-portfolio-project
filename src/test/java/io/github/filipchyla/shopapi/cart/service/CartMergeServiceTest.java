package io.github.filipchyla.shopapi.cart.service;

import io.github.filipchyla.shopapi.cart.AddItemOutcome;
import io.github.filipchyla.shopapi.cart.dto.AddCartItemRequest;
import io.github.filipchyla.shopapi.cart.dto.CartItemResponse;
import io.github.filipchyla.shopapi.cart.dto.CartMergeResponse;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.cart.exception.CartItemLimitExceededException;
import io.github.filipchyla.shopapi.cart.exception.InsufficientStockException;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartMergeServiceTest {

    @Mock
    private GuestCartService guestCartService;
    @Mock
    private CartService cartService;

    @InjectMocks
    private CartMergeService cartMergeService;

    private User user;
    private String cartToken;
    private CartResponse emptyResponse;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        cartToken = UUID.randomUUID().toString();
        emptyResponse = new CartResponse(UUID.randomUUID(), List.of(), BigDecimal.ZERO);
    }

    private CartItemResponse itemResponse(UUID productId, int quantity) {
        BigDecimal lineTotal = BigDecimal.TEN.multiply(BigDecimal.valueOf(quantity));
        return new CartItemResponse(
                productId, "Product " + productId, quantity,
                BigDecimal.TEN, BigDecimal.TEN, false, lineTotal
        );
    }

    @Nested
    class NoGuestToken {
        @Test
        void merge_ShouldReturnCurrentCart_WhenNoCartTokenPresent() {
            // Given
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            CartMergeResponse result = cartMergeService.merge(null, user);

            // Then
            assertThat(result.skippedItems()).isEmpty();
            verifyNoInteractions(guestCartService);
        }
    }

    @Nested
    class HappyPath {
        @Test
        void merge_ShouldAddAllItemsAndClearGuestCart_WhenEverythingSucceeds() {
            // Given
            UUID p1 = UUID.randomUUID();
            UUID p2 = UUID.randomUUID();
            CartResponse guestCart = new CartResponse(
                    null, List.of(itemResponse(p1, 2), itemResponse(p2, 1)), BigDecimal.valueOf(30));

            when(guestCartService.getCart(cartToken)).thenReturn(guestCart);
            when(cartService.tryAddItem(eq(user), any(AddCartItemRequest.class))).thenReturn(new AddItemOutcome.Added(emptyResponse));
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            CartMergeResponse result = cartMergeService.merge(cartToken, user);

            // Then
            assertThat(result.skippedItems()).isEmpty();
            verify(cartService, times(2)).tryAddItem(eq(user), any(AddCartItemRequest.class));
            verify(guestCartService).clearCart(cartToken);
        }

        @Test
        void merge_ShouldPassCorrectProductIdAndQuantity_ForEachGuestItem() {
            // Given
            UUID p1 = UUID.randomUUID();
            CartResponse guestCart = new CartResponse(null, List.of(itemResponse(p1, 4)), BigDecimal.valueOf(40));

            when(guestCartService.getCart(cartToken)).thenReturn(guestCart);
            when(cartService.tryAddItem(eq(user), any(AddCartItemRequest.class))).thenReturn(new AddItemOutcome.Added(emptyResponse));
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            cartMergeService.merge(cartToken, user);

            // Then
            verify(cartService).tryAddItem(user, new AddCartItemRequest(p1, 4));
        }

        @Test
        void merge_ShouldClearGuestCartAndSkipAddItem_WhenGuestCartHasNoItems() {
            // Given
            CartResponse emptyGuestCart = new CartResponse(null, List.of(), BigDecimal.ZERO);
            when(guestCartService.getCart(cartToken)).thenReturn(emptyGuestCart);
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            CartMergeResponse result = cartMergeService.merge(cartToken, user);

            // Then
            assertThat(result.skippedItems()).isEmpty();
            verify(cartService, never()).addItem(any(), any());
            verify(guestCartService).clearCart(cartToken);
        }
    }

    @Nested
    class PartialFailure {
        @Test
        void merge_ShouldSkipItemsThatFailWithBadRequest_ButStillClearGuestCart() {
            // Given
            UUID p1 = UUID.randomUUID();
            CartResponse guestCart = new CartResponse(null, List.of(itemResponse(p1, 25)), BigDecimal.valueOf(250));

            when(guestCartService.getCart(cartToken)).thenReturn(guestCart);
            when(cartService.tryAddItem(eq(user), any(AddCartItemRequest.class)))
                    .thenReturn(new AddItemOutcome.Rejected(new CartItemLimitExceededException("Limit exceeded")));
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            CartMergeResponse result = cartMergeService.merge(cartToken, user);

            // Then
            assertThat(result.skippedItems()).hasSize(1);
            assertThat(result.skippedItems().getFirst().reason()).contains("Limit exceeded");
            verify(guestCartService).clearCart(cartToken);
        }

        @Test
        void merge_ShouldSkipItemsThatFailWithConflict_ButStillClearGuestCart() {
            // Given
            UUID p1 = UUID.randomUUID();
            CartResponse guestCart = new CartResponse(null, List.of(itemResponse(p1, 5)), BigDecimal.valueOf(50));

            when(guestCartService.getCart(cartToken)).thenReturn(guestCart);
            when(cartService.tryAddItem(eq(user), any(AddCartItemRequest.class)))
                    .thenReturn(new AddItemOutcome.Rejected(new InsufficientStockException("Not enough stock")));
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            CartMergeResponse result = cartMergeService.merge(cartToken, user);

            // Then
            assertThat(result.skippedItems()).hasSize(1);
            assertThat(result.skippedItems().getFirst().reason()).contains("Not enough stock");
            verify(guestCartService).clearCart(cartToken);
        }

        @Test
        void merge_ShouldSkipItemsThatFailWithProductNotFound_ButStillClearGuestCart() {
            // Given
            UUID p1 = UUID.randomUUID();
            CartResponse guestCart = new CartResponse(null, List.of(itemResponse(p1, 1)), BigDecimal.TEN);

            when(guestCartService.getCart(cartToken)).thenReturn(guestCart);
            when(cartService.tryAddItem(eq(user), any(AddCartItemRequest.class)))
                    .thenReturn(new AddItemOutcome.Rejected(new ProductNotFoundException("Product deleted")));
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            CartMergeResponse result = cartMergeService.merge(cartToken, user);

            // Then
            assertThat(result.skippedItems()).hasSize(1);
            assertThat(result.skippedItems().getFirst().reason()).contains("Product deleted");
            verify(guestCartService).clearCart(cartToken);
        }

        @Test
        void merge_ShouldMergeSuccessfulItemsAndSkipFailingOnes_WhenMixed() {
            // Given
            UUID ok = UUID.randomUUID();
            UUID failing = UUID.randomUUID();
            CartResponse guestCart = new CartResponse(
                    null, List.of(itemResponse(ok, 1), itemResponse(failing, 5)), BigDecimal.valueOf(60));

            when(guestCartService.getCart(cartToken)).thenReturn(guestCart);
            when(cartService.tryAddItem(user, new AddCartItemRequest(ok, 1)))
                    .thenReturn(new AddItemOutcome.Added(emptyResponse));
            when(cartService.tryAddItem(user, new AddCartItemRequest(failing, 5)))
                    .thenReturn(new AddItemOutcome.Rejected(new InsufficientStockException("Not enough stock")));
            when(cartService.getCart(user)).thenReturn(emptyResponse);

            // When
            CartMergeResponse result = cartMergeService.merge(cartToken, user);

            // Then
            assertThat(result.skippedItems()).hasSize(1);
            assertThat(result.skippedItems().getFirst().reason()).contains("Not enough stock");
            verify(cartService).tryAddItem(user, new AddCartItemRequest(ok, 1));
            verify(guestCartService).clearCart(cartToken);
        }
    }
}