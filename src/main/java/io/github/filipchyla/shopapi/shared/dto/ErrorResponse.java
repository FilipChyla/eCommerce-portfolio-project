package io.github.filipchyla.shopapi.shared.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Response containing information about an error")
public record ErrorResponse(
        @Schema(
                description = "HTTP status code",
                example = "404"
        )
        int status,

        @Schema(
                description = "Description of the error",
                example = "Product with the specified ID was not found"
        )
        String message,

        @Schema(
                description = "Date and time when the error occurred",
                example = "2026-08-27T12:30:00"
        )
        Instant timestamp
) {
}
