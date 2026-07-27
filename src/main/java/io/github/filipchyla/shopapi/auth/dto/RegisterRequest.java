package io.github.filipchyla.shopapi.auth.dto;

import io.github.filipchyla.shopapi.shared.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank(message = "Email should not be blank")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Password should not be blank")
        @ValidPassword
        String password
) {}