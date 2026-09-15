package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.dto.AddCartItemRequest;
import io.github.filipchyla.shopapi.cart.dto.CartActionResult;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.cart.dto.UpdateCartItemRequest;
import io.github.filipchyla.shopapi.cart.service.CartService;
import io.github.filipchyla.shopapi.cart.service.GuestCartService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.github.filipchyla.shopapi.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class CartFacade {
    private final CartService cartService;
    private final GuestCartService guestCartService;

    public CartResponse getCart(UserPrincipal principal, String cartToken) {
        return execute(principal, cartToken, cartService::getCart, guestCartService::getCart);
    }

    public CartActionResult addItem(UserPrincipal principal, String cartToken, AddCartItemRequest request) {
        return execute(principal, cartToken,
                user -> new CartActionResult(cartService.addItem(user, request), null),
                token -> addGuestItem(token, request));
    }

    public CartResponse updateItemQuantity(UserPrincipal principal, String cartToken, UUID productId, UpdateCartItemRequest request) {
        return execute(principal, cartToken,
                user -> cartService.updateItemQuantity(user, productId, request),
                token -> guestCartService.updateItemQuantity(token, productId, request));
    }

    public CartResponse removeItem(UserPrincipal principal, String cartToken, UUID productId) {
        return execute(principal, cartToken,
                user -> cartService.removeItem(user, productId),
                token -> guestCartService.removeItem(token, productId));
    }

    public void clearCart(UserPrincipal principal, String cartToken) {
        execute(principal, cartToken,
                user -> { cartService.clearCart(user); return null; },
                token -> { guestCartService.clearCart(token); return null; });
    }

    private CartActionResult addGuestItem(String cartToken, AddCartItemRequest request) {
        boolean needsNewToken = cartToken == null;
        String resolvedToken = needsNewToken ? UUID.randomUUID().toString() : cartToken;
        CartResponse cart = guestCartService.addItem(resolvedToken, request);
        return new CartActionResult(cart, needsNewToken ? resolvedToken : null);
    }

    private <T> T execute(UserPrincipal principal, String cartToken,
                          Function<User, T> asAuthenticated,
                          Function<String, T> asGuest) {
        return principal != null
                ? asAuthenticated.apply(principal.user())
                : asGuest.apply(cartToken);
    }
}