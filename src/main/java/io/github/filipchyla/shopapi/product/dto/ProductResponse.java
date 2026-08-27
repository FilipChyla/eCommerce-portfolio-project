package io.github.filipchyla.shopapi.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response containing details of a product")
public record ProductResponse(
        @Schema(
                description = "Unique identifier of the product",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Product name",
                example = "Wireless Mouse"
        )
        String name,

        @Schema(
                description = "Detailed product description",
                example = "Ergonomic wireless mouse with USB-C charging"
        )
        String description,

        @Schema(
                description = "Product price",
                example = "129.99"
        )
        BigDecimal price,

        @Schema(
                description = "Stock quantity",
                example = "50"
        )
        Integer stockQuantity,

        @Schema(
                description = "Name of the category this product belongs to",
                example = "Electronics"
        )
        String categoryName,

        @Schema(
                description = "Date and time when the product was created",
                example = "2026-08-27T12:30:00Z"
        )
        Instant createdAt
) {
}
