package io.github.filipchyla.shopapi.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Request containing data required to create a new product")
public record CreateProductRequest(
        @Schema(
                description = "Product name",
                example = "Wireless Mouse"
        )
        @NotBlank(message = "Product name should not be blank")
        String name,

        @Schema(
                description = "Detailed product description",
                example = "Ergonomic wireless mouse with USB-C charging"
        )
        @NotBlank(message = "Product description should not be blank")
        String description,

        @Schema(
                description = "Product price",
                example = "129.99")
        @NotNull(message = "Product price should not be null")
        @Positive(message = "Product price should be positive")
        BigDecimal price,

        @Schema(
                description = "Initial stock quantity",
                example = "50"
        )
        @PositiveOrZero(message = "Product stock quantity should be positive")
        @NotNull(message = "Product stock quantity should not be null")
        Integer stockQuantity,

        @Schema(
                description = "ID of the category this product belongs to",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
        )
        @NotNull(message = "Product category should not be null")
        UUID categoryId
) {
}
