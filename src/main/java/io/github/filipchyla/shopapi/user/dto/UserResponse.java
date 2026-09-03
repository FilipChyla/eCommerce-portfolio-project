package io.github.filipchyla.shopapi.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Response containing user details")
public record UserResponse(

        @Schema(
                description = "Unique identifier of the user",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        UUID id,

        @Schema(
                description = "User's email address",
                example = "john.doe@example.com"
        )
        String email,

        @Schema(
                description = "User's first name",
                example = "John"
        )
        String firstName,

        @Schema(
                description = "User's last name",
                example = "Doe"
        )
        String lastName,

        @Schema(
                description = "User's phone number",
                example = "+48123456789"
        )
        String phone,

        @Schema(
                description = "Date and time when the user was created",
                example = "2026-08-27T12:30:00"
        )
        Instant createdAt
) {
}
