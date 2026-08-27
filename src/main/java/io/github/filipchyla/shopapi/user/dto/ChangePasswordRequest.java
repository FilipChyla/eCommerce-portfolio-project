package io.github.filipchyla.shopapi.user.dto;

import io.github.filipchyla.shopapi.shared.validation.ValidPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request containing required password information to update")
public record ChangePasswordRequest(
        @Schema(
                description = "User's current password",
                example = "Password1!"
        )
        @NotBlank(message = "Password should not be blank")
        String currentPassword,

        @Schema(
                description = "User's password, must have at least 8 characters long and contain" +
                        " an uppercase letter, a lowercase letter, a digit and a special character",
                example = "#StrongerP@ssword!1"
        )
        @NotBlank(message = "Password should not be blank")
        @ValidPassword
        String newPassword
) {
}
