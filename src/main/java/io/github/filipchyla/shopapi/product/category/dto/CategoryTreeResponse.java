package io.github.filipchyla.shopapi.product.category.dto;

import io.github.filipchyla.shopapi.product.category.Category;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CategoryTreeResponse(
        UUID id,
        String name,
        Instant createdAt,
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
