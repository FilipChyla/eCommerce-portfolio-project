package io.github.filipchyla.shopapi.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatchUserRequest(
        @Size(max = 50)
        String firstName,
        @Size(max = 50)
        String lastName,
        @Size(max = 20)
        @Pattern(
                regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "Phone number must be a valid international number"
        )
        String phone
) {
}
