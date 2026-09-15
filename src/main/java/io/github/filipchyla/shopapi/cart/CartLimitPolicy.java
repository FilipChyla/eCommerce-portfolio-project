package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.exception.CartItemLimitExceededException;
import io.github.filipchyla.shopapi.cart.exception.InsufficientStockException;
import io.github.filipchyla.shopapi.product.Product;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CartLimitPolicy {

    private static final int MAX_QUANTITY_PER_ITEM = 20;
    private static final int MAX_DISTINCT_ITEMS = 50;

    public void validateAddition(
            Product product,
            int requestedQuantity,
            boolean newItem,
            int distinctItemCount
    ) {
        validateQuantity(product.getId(), requestedQuantity);
        validateDistinctItems(newItem, distinctItemCount);
        validateStock(product, requestedQuantity);
    }

    public void validateQuantityUpdate(Product product, int newQuantity) {
        validateQuantity(product.getId(), newQuantity);
        validateStock(product, newQuantity);
    }

    private void validateQuantity(UUID productId, int quantity) {
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new CartItemLimitExceededException(
                    "Cannot have more than " + MAX_QUANTITY_PER_ITEM
                            + " units of product " + productId + " in the cart"
            );
        }
    }

    private void validateDistinctItems(boolean newItem, int distinctItemCount) {
        if (newItem && distinctItemCount >= MAX_DISTINCT_ITEMS) {
            throw new CartItemLimitExceededException(
                    "Cart cannot contain more than "
                            + MAX_DISTINCT_ITEMS + " distinct items"
            );
        }
    }

    private void validateStock(Product product, int requestedQuantity) {
        if (product.getStockQuantity() < requestedQuantity) {
            throw new InsufficientStockException(
                    "Not enough stock for product " + product.getId()
                            + ": requested " + requestedQuantity
                            + ", available " + product.getStockQuantity()
            );
        }
    }
}
