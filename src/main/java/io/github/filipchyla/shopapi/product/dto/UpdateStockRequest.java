package io.github.filipchyla.shopapi.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;

@Schema(description = "Request containing stock difference required to adjusting product's stock")
public record UpdateStockRequest(
        @Schema(
                description = "Stock difference, positive adds, negative subtracts",
                example = "50"
        )
        @Digits(
                message = "Product stock difference should have maximum number of integral digits of 6 and no fractional digits"
                , integer = 6
                , fraction = 0
        )
        Integer difference
) {
}
