package io.github.filipchyla.shopapi.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "A single line item in a cart")
public record CartItemResponse(
        @Schema(description = "Product identifier") UUID productId,
        @Schema(description = "Product name") String productName,
        @Schema(description = "Quantity in cart") Integer quantity,
        @Schema(description = "Unit price captured when the item was added") BigDecimal unitPriceSnapshot,
        @Schema(description = "Current live price of the product") BigDecimal currentPrice,
        @Schema(description = "True if currentPrice differs from unitPriceSnapshot") boolean priceChanged,
        @Schema(description = "quantity * currentPrice") BigDecimal lineTotal
) {
}
