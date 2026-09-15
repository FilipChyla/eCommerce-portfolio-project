package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.dto.*;
import io.github.filipchyla.shopapi.cart.service.CartMergeService;
import io.github.filipchyla.shopapi.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Cart", description = "Operations on the current user's or guest's shopping cart")
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartFacade cartFacade;
    private final CartMergeService cartMergeService;
    private final CartTokenCookieFactory cartTokenCookieFactory;

    @Operation(summary = "Get the current cart")
    @GetMapping
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal,
                                                 @CookieValue(name = "${app.cart-token.cookie-name}", required = false) String cartToken) {
        CartResponse result = cartFacade.getCart(principal, cartToken);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Add a product to the cart",
            description = "Sets the cart_token cookie if this is the first item added by a guest")
    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(@AuthenticationPrincipal UserPrincipal principal,
                                                 @CookieValue(name = "${app.cart-token.cookie-name}", required = false) String cartToken,
                                                 @RequestBody @Valid AddCartItemRequest request) {
        CartActionResult result = cartFacade.addItem(principal, cartToken, request);

        ResponseEntity.BodyBuilder response = ResponseEntity.ok();
        if (result.cartTokenToSet() != null) {
            response.header(
                    HttpHeaders.SET_COOKIE,
                    cartTokenCookieFactory.create(result.cartTokenToSet()).toString()
            );
        }
        return response.body(result.cart());
    }

    @Operation(summary = "Change the quantity of a product already in the cart")
    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartResponse> updateItemQuantity(@AuthenticationPrincipal UserPrincipal principal,
                                                            @CookieValue(name = "${app.cart-token.cookie-name}", required = false) String cartToken,
                                                            @PathVariable UUID productId,
                                                            @RequestBody @Valid UpdateCartItemRequest request) {
        CartResponse result = cartFacade.updateItemQuantity(principal, cartToken, productId, request);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Remove a product from the cart")
    @DeleteMapping("/items/{productId}")
    public ResponseEntity<CartResponse> removeItem(@AuthenticationPrincipal UserPrincipal principal,
                                                    @CookieValue(name = "${app.cart-token.cookie-name}", required = false) String cartToken,
                                                    @PathVariable UUID productId) {
        CartResponse result = cartFacade.removeItem(principal, cartToken, productId);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Clear the entire cart")
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal UserPrincipal principal,
                                           @CookieValue(name = "${app.cart-token.cookie-name}", required = false) String cartToken) {
        cartFacade.clearCart(principal, cartToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cartTokenCookieFactory.clear().toString())
                .build();
    }

    @Operation(summary = "Merge the guest cart into the authenticated user's cart",
            description = "Call right after login if a cart_token cookie is present")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/merge")
    public ResponseEntity<CartMergeResponse> mergeCart(@AuthenticationPrincipal UserPrincipal principal,
                                                        @CookieValue(name = "${app.cart-token.cookie-name}", required = false) String cartToken) {
        CartMergeResponse result = cartMergeService.merge(cartToken, principal.user());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cartTokenCookieFactory.clear().toString())
                .body(result);
    }
}
