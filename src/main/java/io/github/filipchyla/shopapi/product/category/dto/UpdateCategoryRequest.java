package io.github.filipchyla.shopapi.product.category.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCategoryRequest(
        @Size(max = 100)
        String categoryName,
        UUID parentId
) {
}