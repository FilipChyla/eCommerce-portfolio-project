package io.github.filipchyla.shopapi.cart.domain;

import io.github.filipchyla.shopapi.cart.exception.CartItemNotFoundException;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.*;

@Getter
@NoArgsConstructor
public class GuestCart {
    private final List<GuestCartItem> items = new ArrayList<>();

    public List<GuestCartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public GuestCartItem findItemByProductId(UUID productId) {
        return items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public int quantityIfAdded(UUID productId, int delta) {
        GuestCartItem existing = findItemByProductId(productId);
        return (existing != null ? existing.getQuantity() : 0) + delta;
    }

    public void addOrIncreaseItem(UUID productId, int quantity, BigDecimal unitPrice) {
        GuestCartItem existing = findItemByProductId(productId);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + quantity);
        } else {
            items.add(new GuestCartItem(productId, quantity, unitPrice));
        }
    }

    public void removeItem(UUID productId) {
        GuestCartItem item = findItemByProductId(productId);
        if (item == null) {
            throw new CartItemNotFoundException("Product " + productId + " is not in the cart");
        }
        items.remove(item);
    }

    public boolean removeStaleItems(Set<UUID> validProductIds) {
        return items.removeIf(i -> !validProductIds.contains(i.getProductId()));
    }

    public int distinctItemCount() {
        return items.size();
    }
}
