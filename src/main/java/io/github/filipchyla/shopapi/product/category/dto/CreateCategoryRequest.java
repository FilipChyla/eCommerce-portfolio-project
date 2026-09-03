package io.github.filipchyla.shopapi.product.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request containing data required to create a new category")
public record CreateCategoryRequest(
        @Schema(
                description = "Name of the category",
                example = "Electronics"
        )
        @NotBlank(message = "Category name should not be blank")
        @Size(max = 100, min = 3, message = "Category name should be between 3 and 100 characters")
        String name,

        @Schema(
                description = "Unique identifier of the parent category. Null if the category is a root category",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID parentId
) {
}
