package io.github.filipchyla.shopapi.auth.dto;

import io.github.filipchyla.shopapi.shared.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request containing user credentials required for registration")
public record RegisterRequest(
        @Schema(
                description = "User's email address",
                example = "john.doe@example.com"
        )
        @NotBlank(message = "Email should not be blank")
        @Email(message = "Email should be valid")
        String email,

        @Schema(
                description = "User's password, must have at least 8 characters long and contain" +
                        " an uppercase letter, a lowercase letter, a digit and a special character",
                example = "Password!1"
        )
        @NotBlank(message = "Password should not be blank")
        @ValidPassword
        String password
) {}