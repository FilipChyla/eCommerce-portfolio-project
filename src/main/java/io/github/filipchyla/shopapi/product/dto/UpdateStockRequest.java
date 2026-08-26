package io.github.filipchyla.shopapi.product.dto;

import jakarta.validation.constraints.Digits;

public record UpdateStockRequest(
        @Digits(message = "Product stock difference should have maximum number of integral digits of 6 and no fractional digits", integer = 6, fraction = 0)
        Integer difference
) {
}
