package io.github.filipchyla.shopapi.product.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response containing details of a category")
public record SingleCategoryResponse(
        @Schema(
                description = "Unique identifier of the category",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "Name of the category",
                example = "Electronics"
        )
        @NotBlank(message = "Category name should not be blank")
        String name,

        @Schema(
                description = "Date and time when the category was created",
                example = "2026-08-27T12:30:00Z"
        )
        Instant createdAt,

        @Schema(
                description = "Unique identifier of the parent category",
                example = "650e8410-e29b-41e4-a716-446655442200"
        )
        UUID parentId
) {}
