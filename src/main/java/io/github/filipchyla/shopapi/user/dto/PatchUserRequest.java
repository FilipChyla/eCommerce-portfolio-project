package io.github.filipchyla.shopapi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request containing user information to update")
public record PatchUserRequest(
        @Schema(
                description = "User's first name",
                example = "John"
        )
        @Size(max = 50)
        String firstName,

        @Schema(
                description = "User's last name",
                example = "Doe"
        )
        @Size(max = 50)
        String lastName,

        @Schema(
                description = "User's phone number",
                example = "+48123456789"
        )
        @Size(max = 20)
        @Pattern(
                regexp = "^$|^\\+?[1-9]\\d{7,14}$",
                message = "Phone number must be a valid international number"
        )
        String phone
) {
}
