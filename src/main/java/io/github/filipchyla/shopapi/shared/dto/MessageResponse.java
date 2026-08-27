package io.github.filipchyla.shopapi.shared.dto;


import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing a message")
public record MessageResponse(
        @Schema(
                description = "Response message",
                example = "Product deleted successfully"
        )
        String message
) {
}
