package io.github.filipchyla.shopapi.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Response containing the current state of a cart")
public record CartResponse(
        @Schema(description = "Cart identifier (null for a guest/Redis-backed cart)") UUID id,
        @Schema(description = "Items currently in the cart") List<CartItemResponse> items,
        @Schema(description = "Sum of all line totals", example = "159.98") BigDecimal totalPrice
) {
}
