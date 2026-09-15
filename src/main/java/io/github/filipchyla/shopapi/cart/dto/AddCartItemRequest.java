package io.github.filipchyla.shopapi.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "Request to add a product to the cart")
public record AddCartItemRequest(
        @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
        @NotNull(message = "productId is required")
        UUID productId,

        @Schema(example = "2")
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 20, message = "quantity cannot exceed 20 per line item")
        Integer quantity
) {
}
