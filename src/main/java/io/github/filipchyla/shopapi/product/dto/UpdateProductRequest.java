package io.github.filipchyla.shopapi.product.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductRequest(
        @Size(min = 1, message = "Pole nie może być puste")
        String name,
        @Size(min = 1, message = "Pole nie może być puste")
        String description,
        @PositiveOrZero(message = "Product price should be positive or zero")
        BigDecimal price,
        @PositiveOrZero(message = "Product stock quantity should be positive or zero")
        Integer stockQuantity,
        UUID categoryId) {
}
