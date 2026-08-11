package io.github.filipchyla.shopapi.product.category.dto;

import io.github.filipchyla.shopapi.product.category.Category;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        Instant createdAt,
        List<CategoryResponse> children
) {
    public static CategoryResponse leaf(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getCreatedAt(),
                new ArrayList<>()
        );
    }
}
