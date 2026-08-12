package io.github.filipchyla.shopapi.product.dto;

import jakarta.validation.constraints.Positive;

public record UpdateStockRequest(
        @Positive
        Integer quantity
) {
}
