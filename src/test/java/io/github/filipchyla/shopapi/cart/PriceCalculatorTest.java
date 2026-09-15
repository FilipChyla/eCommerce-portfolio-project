package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.dto.CartItemResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PriceCalculatorTest {

    @Nested
    class LineTotal {

        @Test
        void lineTotal_ShouldCalculateCorrectTotal() {
            // Given
            BigDecimal currentPrice = new BigDecimal("19.99");
            int quantity = 3;

            // When
            BigDecimal result = PriceCalculator.lineTotal(currentPrice, quantity);

            // Then
            assertThat(result).isEqualByComparingTo("59.97");
        }

        @Test
        void lineTotal_ShouldReturnZero_WhenQuantityIsZero() {
            // Given
            BigDecimal currentPrice = new BigDecimal("19.99");
            int quantity = 0;

            // When
            BigDecimal result = PriceCalculator.lineTotal(currentPrice, quantity);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    class IsPriceChanged {

        @Test
        void isPriceChanged_ShouldReturnTrue_WhenPricesDiffer() {
            // Given
            BigDecimal snapshot = new BigDecimal("20.00");
            BigDecimal current = new BigDecimal("25.00");

            // When
            boolean result = PriceCalculator.isPriceChanged(snapshot, current);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        void isPriceChanged_ShouldReturnFalse_WhenPricesEqualRegardlessOfScale() {
            // Given
            BigDecimal snapshot = new BigDecimal("20.0");
            BigDecimal current = new BigDecimal("20.000");

            // When
            boolean result = PriceCalculator.isPriceChanged(snapshot, current);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    class TotalPrice {

        @Test
        void totalPrice_ShouldSumLineTotalsCorrectly() {
            // Given
            CartItemResponse item1 = new CartItemResponse(
                    UUID.randomUUID(), "Product 1", 2,
                    new BigDecimal("10.00"), new BigDecimal("10.00"), false, new BigDecimal("20.00")
            );
            CartItemResponse item2 = new CartItemResponse(
                    UUID.randomUUID(), "Product 2", 1,
                    new BigDecimal("15.50"), new BigDecimal("15.50"), false, new BigDecimal("15.50")
            );

            // When
            BigDecimal result = PriceCalculator.totalPrice(List.of(item1, item2));

            // Then
            assertThat(result).isEqualByComparingTo("35.50");
        }

        @Test
        void totalPrice_ShouldReturnZero_WhenItemListIsEmpty() {
            // Given
            List<CartItemResponse> emptyItems = List.of();

            // When
            BigDecimal result = PriceCalculator.totalPrice(emptyItems);

            // Then
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
