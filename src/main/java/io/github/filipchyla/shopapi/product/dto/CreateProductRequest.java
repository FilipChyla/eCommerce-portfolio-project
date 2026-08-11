package io.github.filipchyla.shopapi.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "Product name should not be blank")
        String name,
        @NotBlank(message = "Product description should not be blank")
        String description,
        @NotNull(message = "Product price should not be null")
        BigDecimal price,
        @Size(message = "Product stock quantity should be positive")
        Integer stockQuantity,
        @NotNull(message = "Product category should not be null")
        UUID categoryId) {
}
