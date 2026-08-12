package io.github.filipchyla.shopapi.product;

import jakarta.validation.constraints.Positive;

public record UpdateStockRequest(
        @Positive
        Integer quantity
) {
}
