package io.github.filipchyla.shopapi.auth.dto;

import java.time.Instant;

public record RefreshTokenData(
        String userId,
        String familyId,
        Instant issuedAt
) {
}
