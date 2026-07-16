package io.github.filipchyla.shopapi.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest (
    @NotBlank(message = "Email should not be blank")
    String email,
    @NotBlank(message = "Password should not be blank")
    String password
) {}
