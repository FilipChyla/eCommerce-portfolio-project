package io.github.filipchyla.shopapi.cart.domain;

import io.github.filipchyla.shopapi.cart.exception.CartItemNotFoundException;
import io.github.filipchyla.shopapi.product.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartDomainTest {

    private Product product;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        product = new Product();
        product.setId(productId);
        product.setName("Keyboard");
        product.setPrice(new BigDecimal("150.00"));
        product.setStockQuantity(10);
    }

    @Nested
    class CartEntity {

        @Test
        void addNewItem_ShouldInitializeItemAndSetSnapshot() {
            // Given
            Cart cart = new Cart();

            // When
            cart.addNewItem(product, 2);

            // Then
            assertThat(cart.getItems()).hasSize(1);
            CartItem item = cart.getItems().getFirst();
            assertThat(item.getProduct()).isSameAs(product);
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("150.00");
            assertThat(item.getCart()).isSameAs(cart);
        }

        @Test
        void addOrIncreaseItem_ShouldAddNewItem_WhenProductNotInCart() {
            // Given
            Cart cart = new Cart();

            // When
            cart.addOrIncreaseItem(product, 3);

            // Then
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(3);
        }

        @Test
        void addOrIncreaseItem_ShouldUpdateQuantity_WhenProductAlreadyInCart() {
            // Given
            Cart cart = new Cart();
            cart.addNewItem(product, 2);

            // When
            cart.addOrIncreaseItem(product, 3);

            // Then
            assertThat(cart.getItems()).hasSize(1);
            assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(5);
        }

        @Test
        void quantityIfAdded_ShouldReturnDelta_WhenProductNotInCart() {
            // Given
            Cart cart = new Cart();

            // When
            int total = cart.quantityIfAdded(productId, 3);

            // Then
            assertThat(total).isEqualTo(3);
        }

        @Test
        void quantityIfAdded_ShouldReturnSum_WhenProductAlreadyInCart() {
            // Given
            Cart cart = new Cart();
            cart.addNewItem(product, 2);

            // When
            int total = cart.quantityIfAdded(productId, 3);

            // Then
            assertThat(total).isEqualTo(5);
        }

        @Test
        void updateItemQuantity_ShouldUpdateQuantity() {
            // Given
            Cart cart = new Cart();
            cart.addNewItem(product, 1);
            CartItem item = cart.findItemByProductId(productId);

            // When
            cart.updateItemQuantity(item, 7);

            // Then
            assertThat(item.getQuantity()).isEqualTo(7);
        }

        @Test
        void removeItem_ShouldRemoveItem_WhenItemExists() {
            // Given
            Cart cart = new Cart();
            cart.addNewItem(product, 1);

            // When
            cart.removeItem(productId);

            // Then
            assertThat(cart.getItems()).isEmpty();
        }

        @Test
        void removeItem_ShouldThrowNotFound_WhenItemNotInCart() {
            // Given
            Cart cart = new Cart();

            // When & Then
            assertThatThrownBy(() -> cart.removeItem(productId))
                    .isInstanceOf(CartItemNotFoundException.class)
                    .hasMessageContaining("Product " + productId + " is not in the cart");
        }

        @Test
        void clear_ShouldRemoveAllItems() {
            // Given
            Cart cart = new Cart();
            cart.addNewItem(product, 2);

            // When
            cart.clear();

            // Then
            assertThat(cart.getItems()).isEmpty();
            assertThat(cart.distinctItemCount()).isEqualTo(0);
        }

        @Test
        void getItems_ShouldReturnUnmodifiableList() {
            // Given
            Cart cart = new Cart();
            cart.addNewItem(product, 1);
            List<CartItem> items = cart.getItems();

            // When & Then
            assertThatThrownBy(() -> items.add(new CartItem()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class GuestCartModel {

        @Test
        void addOrIncreaseItem_ShouldAddNewItem_WhenProductNotInCart() {
            // Given
            GuestCart guestCart = new GuestCart();

            // When
            guestCart.addOrIncreaseItem(productId, 2, new BigDecimal("150.00"));

            // Then
            assertThat(guestCart.getItems()).hasSize(1);
            GuestCartItem item = guestCart.getItems().getFirst();
            assertThat(item.getProductId()).isEqualTo(productId);
            assertThat(item.getQuantity()).isEqualTo(2);
            assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("150.00");
        }

        @Test
        void addOrIncreaseItem_ShouldUpdateQuantity_WhenProductAlreadyInCart() {
            // Given
            GuestCart guestCart = new GuestCart();
            guestCart.addOrIncreaseItem(productId, 2, new BigDecimal("150.00"));

            // When
            guestCart.addOrIncreaseItem(productId, 3, new BigDecimal("150.00"));

            // Then
            assertThat(guestCart.getItems()).hasSize(1);
            assertThat(guestCart.getItems().getFirst().getQuantity()).isEqualTo(5);
        }

        @Test
        void quantityIfAdded_ShouldReturnDelta_WhenProductNotInCart() {
            // Given
            GuestCart guestCart = new GuestCart();

            // When
            int total = guestCart.quantityIfAdded(productId, 4);

            // Then
            assertThat(total).isEqualTo(4);
        }

        @Test
        void quantityIfAdded_ShouldReturnSum_WhenProductAlreadyInCart() {
            // Given
            GuestCart guestCart = new GuestCart();
            guestCart.addOrIncreaseItem(productId, 3, new BigDecimal("150.00"));

            // When
            int total = guestCart.quantityIfAdded(productId, 2);

            // Then
            assertThat(total).isEqualTo(5);
        }

        @Test
        void removeItem_ShouldRemoveItem_WhenItemExists() {
            // Given
            GuestCart guestCart = new GuestCart();
            guestCart.addOrIncreaseItem(productId, 1, new BigDecimal("150.00"));

            // When
            guestCart.removeItem(productId);

            // Then
            assertThat(guestCart.getItems()).isEmpty();
        }

        @Test
        void removeItem_ShouldThrowNotFound_WhenItemNotInCart() {
            // Given
            GuestCart guestCart = new GuestCart();

            // When & Then
            assertThatThrownBy(() -> guestCart.removeItem(productId))
                    .isInstanceOf(CartItemNotFoundException.class)
                    .hasMessageContaining("Product " + productId + " is not in the cart");
        }

        @Test
        void removeStaleItems_ShouldDropItemsNotInValidSet() {
            // Given
            GuestCart guestCart = new GuestCart();
            UUID validId = UUID.randomUUID();
            UUID staleId = UUID.randomUUID();
            guestCart.addOrIncreaseItem(validId, 1, BigDecimal.TEN);
            guestCart.addOrIncreaseItem(staleId, 2, BigDecimal.ONE);

            // When
            boolean removed = guestCart.removeStaleItems(Set.of(validId));

            // Then
            assertThat(removed).isTrue();
            assertThat(guestCart.getItems()).hasSize(1);
            assertThat(guestCart.getItems().getFirst().getProductId()).isEqualTo(validId);
        }

        @Test
        void getItems_ShouldReturnUnmodifiableList() {
            // Given
            GuestCart guestCart = new GuestCart();
            guestCart.addOrIncreaseItem(productId, 1, BigDecimal.TEN);
            List<GuestCartItem> items = guestCart.getItems();

            // When & Then
            assertThatThrownBy(() -> items.add(new GuestCartItem(UUID.randomUUID(), 1, BigDecimal.ONE)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
