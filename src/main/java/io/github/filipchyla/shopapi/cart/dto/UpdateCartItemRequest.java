package io.github.filipchyla.shopapi.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to change the quantity of an existing cart line item")
public record UpdateCartItemRequest(
        @Schema(example = "3")
        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1 - use DELETE to remove the item")
        @Max(value = 20, message = "quantity cannot exceed 20 per line item")
        Integer quantity
) {
}
