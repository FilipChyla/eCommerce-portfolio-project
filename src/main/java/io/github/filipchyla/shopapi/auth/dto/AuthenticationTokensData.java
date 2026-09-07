package io.github.filipchyla.shopapi.auth.dto;

import org.springframework.http.ResponseCookie;

public record AuthenticationTokensData(
        AccessTokenData accessToken,
        ResponseCookie refreshTokenCookie
) {}
