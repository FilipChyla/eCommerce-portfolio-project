package io.github.filipchyla.shopapi.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request containing data required to update an existing product")
public record UpdateProductRequest(
        @Schema(
                description = "Product name",
                example = "Wireless Mouse"
        )
        @Size(min = 1, message = "Product name cannot be empty")
        String name,

        @Schema(
                description = "Detailed product description",
                example = "Ergonomic wireless mouse with USB-C charging"
        )
        @Size(min = 1, message = "Product description cannot be empty")
        String description,

        @Schema(
                description = "Product price",
                example = "129.99")
        @PositiveOrZero(message = "Product price should be positive or zero")
        BigDecimal price,

        @Schema(
                description = "New stock quantity",
                example = "50"
        )
        @PositiveOrZero(message = "Product stock quantity should be positive or zero")
        Integer stockQuantity,

        @Schema(
                description = "ID of the category this product belongs to",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        UUID categoryId) {
}
