package io.github.filipchyla.shopapi.cart;

import io.github.filipchyla.shopapi.cart.dto.CartItemResponse;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.util.List;

@UtilityClass
public final class PriceCalculator {

    public static BigDecimal lineTotal(BigDecimal currentPrice, int quantity) {
        return currentPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public static boolean isPriceChanged(BigDecimal unitPriceSnapshot, BigDecimal currentPrice) {
        return unitPriceSnapshot.compareTo(currentPrice) != 0;
    }

    public static BigDecimal totalPrice(List<CartItemResponse> items) {
        return items.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
