package io.github.filipchyla.shopapi.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Item that couldn't be merged and reason for it")
public record SkippedCartItem(
        @Schema(description = "Item id")
        UUID productId,
        @Schema(description = "Item name")
        String productName,
        @Schema(description = "Failure reason")
        String reason
) { }
