package io.github.filipchyla.shopapi.product.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @Size(min = 1, message = "Product name cannot be empty")
        String name,
        @Size(min = 1, message = "Product description cannot be empty")
        String description,
        @PositiveOrZero(message = "Product price should be positive or zero")
        BigDecimal price,
        @PositiveOrZero(message = "Product stock quantity should be positive or zero")
        Integer stockQuantity,
        UUID categoryId) {
}
