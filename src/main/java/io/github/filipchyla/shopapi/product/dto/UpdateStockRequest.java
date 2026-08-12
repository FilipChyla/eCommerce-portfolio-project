package io.github.filipchyla.shopapi.product.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateStockRequest(
        @PositiveOrZero(message = "Product stock quantity should be positive or zero")
        Integer quantity
) {
}
