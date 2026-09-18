package io.github.filipchyla.shopapi.cart.service;

import io.github.filipchyla.shopapi.cart.AddItemOutcome;
import io.github.filipchyla.shopapi.cart.CartLimitPolicy;
import io.github.filipchyla.shopapi.cart.CartRepository;
import io.github.filipchyla.shopapi.cart.domain.Cart;
import io.github.filipchyla.shopapi.cart.domain.CartItem;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartLimitPolicy cartLimitPolicy;
    private final CartRepository cartRepository;
    private final ProductService productService;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse getCart(User user) {
        return cartMapper.toCartResponse(getOrCreateCart(user));
    }

    @Transactional
    public AddItemOutcome tryAddItem(User user, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(user);

        Product product = productService.findActiveProductById(request.productId()).orElse(null);

        if (product == null) {
            return new AddItemOutcome.Rejected(new ProductNotFoundException("Product not found with id: " + request.productId()));
        }

        boolean isNew = cart.findItemByProductId(product.getId()) == null;
        int requestedQuantity = cart.quantityIfAdded(product.getId(), request.quantity());

        try {
            cartLimitPolicy.validateAddition(product, requestedQuantity, isNew, cart.distinctItemCount());
        } catch (CartItemLimitExceededException | InsufficientStockException e) {
            return new AddItemOutcome.Rejected(e);
        }

        cart.addOrIncreaseItem(product, request.quantity());

        return new AddItemOutcome.Added(cartMapper.toCartResponse(cart));
    }

    @Transactional
    public CartResponse addItem(User user, AddCartItemRequest request) {
        return switch (tryAddItem(user, request)) {
            case AddItemOutcome.Added(CartResponse cart) -> cart;
            case AddItemOutcome.Rejected(RuntimeException cause) -> throw cause;
        };
    }

    @Transactional
    public CartResponse updateItemQuantity(User user, UUID productId, UpdateCartItemRequest request) {
        Cart cart = getOrCreateCart(user);
        CartItem item = cart.findItemByProductId(productId);

        if (item == null) {
            throw new CartItemNotFoundException("Product " + productId + " is not in the cart");
        }

        Product product = item.getProduct();

        cartLimitPolicy.validateQuantityUpdate(product, request.quantity());

        cart.updateItemQuantity(item, request.quantity());
        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(User user, UUID productId) {
        Cart cart = getOrCreateCart(user);
        cart.removeItem(productId);
        return cartMapper.toCartResponse(cart);
    }

    @Transactional
    public void clearCart(User user) {
        getOrCreateCart(user).clear();
    }

    private Cart getOrCreateCart(User user) {
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }
}
