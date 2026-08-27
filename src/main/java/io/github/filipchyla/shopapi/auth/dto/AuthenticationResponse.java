package io.github.filipchyla.shopapi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing JWT token access token")
public record AuthenticationResponse(
        @Schema(
                description = "JWT access token used to authenticate API requests",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwiaWF0IjoxNzU2Mjg1NDAwfQ...",
                format = "jwt"
        )
        String token
) {
}