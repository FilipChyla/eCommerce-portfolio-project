package io.github.filipchyla.shopapi.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request containing user credentials required for authentication")
public record AuthenticationRequest(
        @Schema(
                description = "User's email address",
                example = "john.doe@example.com"
        )
        @NotBlank(message = "Email should not be blank")
        String email,

        @Schema(
                description = "User's password",
                example = "Password!1"
        )
        @NotBlank(message = "Password should not be blank")
        String password
) {
}
