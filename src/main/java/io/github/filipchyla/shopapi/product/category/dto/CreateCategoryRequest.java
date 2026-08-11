package io.github.filipchyla.shopapi.product.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank(message = "Category name should not be blank")
        @Size(max = 100)
        String categoryName,
        UUID parentId
) {
}
