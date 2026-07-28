package io.github.filipchyla.shopapi.user.dto;

import io.github.filipchyla.shopapi.shared.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "Password should not be blank")
        String currentPassword,
        @NotBlank(message = "Password should not be blank")
        @ValidPassword
        String newPassword
) {
}
