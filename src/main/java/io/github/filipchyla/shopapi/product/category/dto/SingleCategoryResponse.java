package io.github.filipchyla.shopapi.product.category.dto;

import java.time.Instant;
import java.util.UUID;

public record SingleCategoryResponse(
        UUID id,
        String name,
        Instant createdAt,
        UUID parentId
) {}
