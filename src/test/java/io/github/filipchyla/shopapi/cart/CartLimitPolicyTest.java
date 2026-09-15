package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.exception.CartItemLimitExceededException;
import io.github.filipchyla.shopapi.cart.exception.InsufficientStockException;
import io.github.filipchyla.shopapi.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartLimitPolicyTest {

    private CartLimitPolicy cartLimitPolicy;
    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        cartLimitPolicy = new CartLimitPolicy();
        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setPrice(BigDecimal.TEN);
        product.setStockQuantity(50);
    }

    @Nested
    class ValidateAddition {

        @Test
        void validateAddition_ShouldPass_WhenWithinQuantityAndDistinctLimitsAndStockSufficient() {
            // Given
            int requestedQuantity = 5;
            boolean newItem = true;
            int distinctItemCount = 10;

            // When & Then
            assertThatCode(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateAddition_ShouldPass_WhenRequestedQuantityEqualsMaxPerItem() {
            // Given
            int requestedQuantity = 20;
            boolean newItem = true;
            int distinctItemCount = 0;

            // When & Then
            assertThatCode(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateAddition_ShouldThrowLimitExceeded_WhenRequestedQuantityExceedsMaxPerItem() {
            // Given
            int requestedQuantity = 21;
            boolean newItem = true;
            int distinctItemCount = 0;

            // When & Then
            assertThatThrownBy(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .isInstanceOf(CartItemLimitExceededException.class)
                    .hasMessageContaining("Cannot have more than 20 units");
        }

        @Test
        void validateAddition_ShouldPass_WhenDistinctItemsReachMaxAllowedBoundaryForNewItem() {
            // Given
            int requestedQuantity = 1;
            boolean newItem = true;
            int distinctItemCount = 49;

            // When & Then
            assertThatCode(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateAddition_ShouldThrowLimitExceeded_WhenNewItemExceedsMaxDistinctItems() {
            // Given
            int requestedQuantity = 1;
            boolean newItem = true;
            int distinctItemCount = 50;

            // When & Then
            assertThatThrownBy(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .isInstanceOf(CartItemLimitExceededException.class)
                    .hasMessageContaining("Cart cannot contain more than 50 distinct items");
        }

        @Test
        void validateAddition_ShouldPass_WhenExistingItemInFullCart() {
            // Given
            int requestedQuantity = 2;
            boolean newItem = false;
            int distinctItemCount = 50;

            // When & Then
            assertThatCode(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateAddition_ShouldPass_WhenRequestedQuantityEqualsAvailableStock() {
            // Given
            product.setStockQuantity(5);
            int requestedQuantity = 5;
            boolean newItem = true;
            int distinctItemCount = 0;

            // When & Then
            assertThatCode(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateAddition_ShouldThrowInsufficientStock_WhenRequestedQuantityExceedsAvailableStock() {
            // Given
            product.setStockQuantity(3);
            int requestedQuantity = 4;
            boolean newItem = true;
            int distinctItemCount = 0;

            // When & Then
            assertThatThrownBy(() -> cartLimitPolicy.validateAddition(product, requestedQuantity, newItem, distinctItemCount))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Not enough stock for product " + productId);
        }
    }

    @Nested
    class ValidateQuantityUpdate {

        @Test
        void validateQuantityUpdate_ShouldPass_WhenValidAndInStock() {
            // Given
            int newQuantity = 10;

            // When & Then
            assertThatCode(() -> cartLimitPolicy.validateQuantityUpdate(product, newQuantity))
                    .doesNotThrowAnyException();
        }

        @Test
        void validateQuantityUpdate_ShouldThrowLimitExceeded_WhenNewQuantityExceedsMaxPerItem() {
            // Given
            int newQuantity = 21;

            // When & Then
            assertThatThrownBy(() -> cartLimitPolicy.validateQuantityUpdate(product, newQuantity))
                    .isInstanceOf(CartItemLimitExceededException.class)
                    .hasMessageContaining("Cannot have more than 20 units");
        }

        @Test
        void validateQuantityUpdate_ShouldThrowInsufficientStock_WhenNewQuantityExceedsStock() {
            // Given
            product.setStockQuantity(2);
            int newQuantity = 5;

            // When & Then
            assertThatThrownBy(() -> cartLimitPolicy.validateQuantityUpdate(product, newQuantity))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessageContaining("Not enough stock for product " + productId);
        }
    }
}
