package io.github.filipchyla.shopapi.cart.domain;

import io.github.filipchyla.shopapi.cart.exception.CartItemNotFoundException;
import io.github.filipchyla.shopapi.product.Product;
import io.github.filipchyla.shopapi.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
@Getter
@NoArgsConstructor
public class Cart {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Setter
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    @Setter
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    @Version
    private Long version;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public CartItem findItemByProductId(UUID productId) {
        return items.stream()
                .filter(i -> i.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    public int quantityIfAdded(UUID productId, int delta) {
        CartItem existing = findItemByProductId(productId);
        return (existing != null ? existing.getQuantity() : 0) + delta;
    }

    public void addOrIncreaseItem(Product product, int quantity) {
        CartItem existing = findItemByProductId(product.getId());
        if (existing != null) {
            updateItemQuantity(existing, existing.getQuantity() + quantity);
        } else {
            addNewItem(product, quantity);
        }
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addNewItem(Product product, int quantity) {
        CartItem item = new CartItem();
        item.setCart(this);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setUnitPriceSnapshot(product.getPrice());
        items.add(item);
    }

    public void updateItemQuantity(CartItem item, int newQuantity) {
        item.setQuantity(newQuantity);
    }

    public void removeItem(UUID productId) {
        CartItem item = findItemByProductId(productId);
        if (item == null) {
            throw new CartItemNotFoundException("Product " + productId + " is not in the cart");
        }
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }

    public int distinctItemCount() {
        return items.size();
    }
}
