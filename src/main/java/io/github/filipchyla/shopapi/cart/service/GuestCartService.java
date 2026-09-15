package io.github.filipchyla.shopapi.cart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.filipchyla.shopapi.cart.CartLimitPolicy;
import io.github.filipchyla.shopapi.cart.domain.GuestCart;
import io.github.filipchyla.shopapi.cart.domain.GuestCartItem;
import io.github.filipchyla.shopapi.cart.dto.AddCartItemRequest;
import io.github.filipchyla.shopapi.cart.dto.CartResponse;
import io.github.filipchyla.shopapi.cart.dto.UpdateCartItemRequest;
import io.github.filipchyla.shopapi.cart.exception.CartItemNotFoundException;
import io.github.filipchyla.shopapi.cart.mapper.GuestCartMapper;
import io.github.filipchyla.shopapi.product.Product;
import io.github.filipchyla.shopapi.product.ProductService;
import io.github.filipchyla.shopapi.product.exception.ProductNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestCartService {
    private static final Duration GUEST_CART_TTL = Duration.ofDays(7);
    private static final String KEY_PREFIX = "guest-cart:";

    private final RedisTemplate<String, String> redis;
    private final ObjectMapper objectMapper;
    private final ProductService productService;
    private final CartLimitPolicy cartLimitPolicy;
    private final GuestCartMapper guestCartMapper;

    public CartResponse getCart(String cartToken) {
        GuestCart cart = loadCart(cartToken);
        Map<UUID, Product> productsInfo = fetchProductInfoForCart(cart.getItems());

        if (cart.removeStaleItems(productsInfo.keySet())) {
            save(cartToken, cart);
        }

        return guestCartMapper.toCartResponse(cart, productsInfo);
    }

    public CartResponse addItem(String cartToken, AddCartItemRequest request) {
        GuestCart cart = loadCart(cartToken);
        Product product = productService.findActiveProductById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + request.productId()));

        boolean isNew = cart.findItemByProductId(product.getId()) == null;
        int requestedQuantity = cart.quantityIfAdded(request.productId(), request.quantity());

        Map<UUID, Product> productsInfo = fetchProductInfoForCart(cart.getItems());
        cart.removeStaleItems(productsInfo.keySet());

        cartLimitPolicy.validateAddition(
                product,
                requestedQuantity,
                isNew,
                cart.distinctItemCount());

        cart.addOrIncreaseItem(product.getId(), request.quantity(), product.getPrice());

        save(cartToken, cart);

        productsInfo.put(product.getId(), product);
        return guestCartMapper.toCartResponse(cart, productsInfo);
    }

    public CartResponse updateItemQuantity(String cartToken, UUID productId, UpdateCartItemRequest request) {
        GuestCart cart = loadCart(cartToken);
        GuestCartItem item = cart.findItemByProductId(productId);
        if (item == null) {
            throw new CartItemNotFoundException("Product " + productId + " is not in the cart");
        }

        Product product = productService.findProductById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + productId));

        cartLimitPolicy.validateQuantityUpdate(product, request.quantity());

        item.setQuantity(request.quantity());

        save(cartToken, cart);

        Map<UUID, Product> productsInfo = fetchProductInfoForCart(cart.getItems());
        return guestCartMapper.toCartResponse(cart, productsInfo);
    }

    public CartResponse removeItem(String cartToken, UUID productId) {
        GuestCart cart = loadCart(cartToken);
        GuestCartItem item = cart.findItemByProductId(productId);

        if (item == null) {
            throw new CartItemNotFoundException("Product " + productId + " is not in the cart");
        }

        cart.removeItem(productId);
        save(cartToken, cart);

        Map<UUID, Product> productsInfo = fetchProductInfoForCart(cart.getItems());
        return guestCartMapper.toCartResponse(cart, productsInfo);
    }

    public void clearCart(String cartToken) {
        if (cartToken != null) {
            redis.delete(key(cartToken));
        }
    }

    private Map<UUID, Product> fetchProductInfoForCart(List<GuestCartItem> items) {
        List<UUID> productIds = items.stream().map(GuestCartItem::getProductId).toList();

        return productService.findAllByIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private GuestCart loadCart(String cartToken) {
        if (cartToken == null) {
            return new GuestCart();
        }

        String json = redis.opsForValue().get(key(cartToken));
        if (json == null) {
            return new GuestCart();
        }

        try {
            return objectMapper.readValue(json, GuestCart.class);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupted guest cart data", e);
        }
    }

    private void save(String cartToken, GuestCart cart) {
        try {
            redis.opsForValue().set(key(cartToken), objectMapper.writeValueAsString(cart), GUEST_CART_TTL);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize guest cart data", e);
        }
    }

    private String key(String cartToken) {
        return KEY_PREFIX + cartToken;
    }
}
