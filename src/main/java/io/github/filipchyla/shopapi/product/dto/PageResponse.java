package io.github.filipchyla.shopapi.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Paginated response containing a list of items and pagination metadata")
public record PageResponse<T>(
        @Schema(
                description = "Items returned for the current page"
        )
        List<T> content,

        @Schema(
                description = "Zero-based page number",
                example = "0"
        )
        int pageNumber,

        @Schema(
                description = "Number of items requested per page",
                example = "20"
        )
        int pageSize,

        @Schema(
                description = "Total number of items across all pages",
                example = "125"
        )
        long totalElements,

        @Schema(
                description = "Total number of pages",
                example = "7"
        )
        int totalPages,

        @Schema(
                description = "Whether the current page is the last page",
                example = "false"
        )
        boolean last
){
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
