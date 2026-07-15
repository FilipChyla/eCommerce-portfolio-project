package io.github.filipchyla.shopapi.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank(message = "Email should not be blank")
        @Email(message = "Email should be valid")
        String email,

        @NotBlank(message = "Password should not be blank")
        @Pattern(regexp = "^(?=.*[A-Z])(?=.*[!@#$&*])(?=.*[0-9])(?=.*[a-z]).{8,}$",
                message = "Password should have at least 8 characters one capital letter, one small letter, one number and" +
                        " one special character")
        String password
) {}