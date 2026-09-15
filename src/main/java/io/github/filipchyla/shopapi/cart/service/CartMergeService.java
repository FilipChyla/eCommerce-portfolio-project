package io.github.filipchyla.shopapi.cart.service;

import io.github.filipchyla.shopapi.cart.dto.*;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import io.github.filipchyla.shopapi.shared.exception.BadRequestException;
import io.github.filipchyla.shopapi.shared.exception.ConflictException;
import io.github.filipchyla.shopapi.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartMergeService {

    private final GuestCartService guestCartService;
    private final CartService cartService;

    public CartMergeResponse merge(String cartToken, User user) {
        if (cartToken == null) {
            return new CartMergeResponse(cartService.getCart(user), List.of());
        }

        CartResponse guestCart = guestCartService.getCart(cartToken);
        List<SkippedCartItem> skipped = new ArrayList<>();

        for (CartItemResponse item : guestCart.items()) {
            try {
                cartService.addItem(user, new AddCartItemRequest(item.productId(), item.quantity()));
            } catch (BadRequestException | ConflictException | ProductNotFoundException e) {
                skipped.add(new SkippedCartItem(item.productId(), item.productName(), e.getMessage()));
            }
        }

        guestCartService.clearCart(cartToken);
        return new CartMergeResponse(cartService.getCart(user), skipped);
    }
}
