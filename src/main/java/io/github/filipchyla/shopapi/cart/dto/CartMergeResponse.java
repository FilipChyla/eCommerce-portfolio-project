package io.github.filipchyla.shopapi.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Result of merging a guest cart into a user's cart")
public record CartMergeResponse(
        @Schema(description = "The user's cart after merging") CartResponse cart,
        @Schema(description = "Items that couldn't be merged") List<SkippedCartItem> skippedItems
) {
}
