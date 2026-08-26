package io.github.filipchyla.shopapi.product.category.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCategoryRequest(
        @Size(max = 100, min = 3, message = "Category name should be between 3 and 100 characters")
        String name,
        UUID parentId
) {
}