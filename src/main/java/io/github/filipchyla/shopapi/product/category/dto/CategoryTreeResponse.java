package io.github.filipchyla.shopapi.product.category.dto;

import io.github.filipchyla.shopapi.product.category.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Schema(description = "Category represented as a tree structure")
public record CategoryTreeResponse(
        @Schema(
                description = "Unique identifier of the category",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Name of the category",
                example = "Electronics"
        )
        String name,

        @Schema(
                description = "Date and time when the category was created",
                example = "2026-08-27T12:30:00Z"
        )
        Instant createdAt,

        @Schema(
                description = "Child categories",
                example = "[]"
        )
        List<CategoryTreeResponse> children
) {
    public static CategoryTreeResponse leaf(Category category) {
        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                new ArrayList<>()
        );
    }
}
