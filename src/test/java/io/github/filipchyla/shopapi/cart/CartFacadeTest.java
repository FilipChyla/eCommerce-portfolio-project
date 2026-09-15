package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.dto.AddCartItemRequest;
import io.github.filipchyla.shopapi.cart.dto.CartActionResult;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.cart.dto.UpdateCartItemRequest;
import io.github.filipchyla.shopapi.cart.service.CartService;
import io.github.filipchyla.shopapi.cart.service.GuestCartService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartFacadeTest {

    @Mock
    private CartService cartService;

    @Mock
    private GuestCartService guestCartService;

    @InjectMocks
    private CartFacade cartFacade;

    private User user;
    private UserPrincipal principal;
    private CartResponse emptyCartResponse;
    private UUID productId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        principal = new UserPrincipal(user);
        productId = UUID.randomUUID();
        emptyCartResponse = new CartResponse(UUID.randomUUID(), List.of(), BigDecimal.ZERO);
    }

    @Nested
    class GetCart {

        @Test
        void getCart_ShouldDelegateToCartService_WhenPrincipalIsPresent() {
            // Given
            when(cartService.getCart(user)).thenReturn(emptyCartResponse);

            // When
            CartResponse result = cartFacade.getCart(principal, "some-token");

            // Then
            assertThat(result).isSameAs(emptyCartResponse);
            verify(cartService).getCart(user);
            verifyNoInteractions(guestCartService);
        }

        @Test
        void getCart_ShouldDelegateToGuestCartService_WhenPrincipalIsNull() {
            // Given
            String guestToken = "guest-token-123";
            when(guestCartService.getCart(guestToken)).thenReturn(emptyCartResponse);

            // When
            CartResponse result = cartFacade.getCart(null, guestToken);

            // Then
            assertThat(result).isSameAs(emptyCartResponse);
            verify(guestCartService).getCart(guestToken);
            verifyNoInteractions(cartService);
        }
    }

    @Nested
    class AddItem {

        @Test
        void addItem_ShouldNotReturnNewToken_WhenAuthenticatedUserAddsItem() {
            // Given
            AddCartItemRequest request = new AddCartItemRequest(productId, 2);
            when(cartService.addItem(user, request)).thenReturn(emptyCartResponse);

            // When
            CartActionResult result = cartFacade.addItem(principal, null, request);

            // Then
            assertThat(result.cart()).isSameAs(emptyCartResponse);
            assertThat(result.cartTokenToSet()).isNull();
            verify(cartService).addItem(user, request);
            verifyNoInteractions(guestCartService);
        }

        @Test
        void addItem_ShouldReturnNewCartToken_WhenGuestWithoutCookieAddsItem() {
            // Given
            AddCartItemRequest request = new AddCartItemRequest(productId, 2);
            when(guestCartService.addItem(anyString(), eq(request))).thenReturn(emptyCartResponse);

            // When
            CartActionResult result = cartFacade.addItem(null, null, request);

            // Then
            assertThat(result.cart()).isSameAs(emptyCartResponse);
            assertThat(result.cartTokenToSet()).isNotNull();
            verify(guestCartService).addItem(eq(result.cartTokenToSet()), eq(request));
            verifyNoInteractions(cartService);
        }

        @Test
        void addItem_ShouldNotReturnNewToken_WhenGuestWithExistingCookieAddsItem() {
            // Given
            String existingToken = "existing-token-abc";
            AddCartItemRequest request = new AddCartItemRequest(productId, 2);
            when(guestCartService.addItem(existingToken, request)).thenReturn(emptyCartResponse);

            // When
            CartActionResult result = cartFacade.addItem(null, existingToken, request);

            // Then
            assertThat(result.cart()).isSameAs(emptyCartResponse);
            assertThat(result.cartTokenToSet()).isNull();
            verify(guestCartService).addItem(existingToken, request);
            verifyNoInteractions(cartService);
        }
    }

    @Nested
    class UpdateItemQuantity {

        @Test
        void updateItemQuantity_ShouldDelegateToCartService_WhenAuthenticated() {
            // Given
            UpdateCartItemRequest request = new UpdateCartItemRequest(4);
            when(cartService.updateItemQuantity(user, productId, request)).thenReturn(emptyCartResponse);

            // When
            CartResponse result = cartFacade.updateItemQuantity(principal, null, productId, request);

            // Then
            assertThat(result).isSameAs(emptyCartResponse);
            verify(cartService).updateItemQuantity(user, productId, request);
            verifyNoInteractions(guestCartService);
        }

        @Test
        void updateItemQuantity_ShouldDelegateToGuestCartService_WhenGuest() {
            // Given
            String token = "guest-token";
            UpdateCartItemRequest request = new UpdateCartItemRequest(4);
            when(guestCartService.updateItemQuantity(token, productId, request)).thenReturn(emptyCartResponse);

            // When
            CartResponse result = cartFacade.updateItemQuantity(null, token, productId, request);

            // Then
            assertThat(result).isSameAs(emptyCartResponse);
            verify(guestCartService).updateItemQuantity(token, productId, request);
            verifyNoInteractions(cartService);
        }
    }

    @Nested
    class RemoveItem {

        @Test
        void removeItem_ShouldDelegateToCartService_WhenAuthenticated() {
            // Given
            when(cartService.removeItem(user, productId)).thenReturn(emptyCartResponse);

            // When
            CartResponse result = cartFacade.removeItem(principal, null, productId);

            // Then
            assertThat(result).isSameAs(emptyCartResponse);
            verify(cartService).removeItem(user, productId);
            verifyNoInteractions(guestCartService);
        }

        @Test
        void removeItem_ShouldDelegateToGuestCartService_WhenGuest() {
            // Given
            String token = "guest-token";
            when(guestCartService.removeItem(token, productId)).thenReturn(emptyCartResponse);

            // When
            CartResponse result = cartFacade.removeItem(null, token, productId);

            // Then
            assertThat(result).isSameAs(emptyCartResponse);
            verify(guestCartService).removeItem(token, productId);
            verifyNoInteractions(cartService);
        }
    }

    @Nested
    class ClearCart {

        @Test
        void clearCart_ShouldDelegateToCartService_WhenPrincipalIsPresent() {
            // When
            cartFacade.clearCart(principal, null);

            // Then
            verify(cartService).clearCart(user);
            verifyNoInteractions(guestCartService);
        }

        @Test
        void clearCart_ShouldDelegateToGuestCartService_WhenPrincipalIsNull() {
            // Given
            String token = "guest-token";

            // When
            cartFacade.clearCart(null, token);

            // Then
            verify(guestCartService).clearCart(token);
            verifyNoInteractions(cartService);
        }
    }
}
