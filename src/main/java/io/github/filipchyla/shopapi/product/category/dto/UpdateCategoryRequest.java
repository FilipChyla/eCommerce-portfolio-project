package io.github.filipchyla.shopapi.product.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request containing data required to update an existing category")
public record UpdateCategoryRequest(
        @Schema(
                description = "Name of the category",
                example = "Electronics"
        )
        @Size(max = 100, min = 3, message = "Category name should be between 3 and 100 characters")
        String name,

        @Schema(
                description = "Unique identifier of the parent category",
                example = "650e8410-e29b-41e4-a716-446655442200"
        )
        UUID parentId
) {
}