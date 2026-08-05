package io.github.filipchyla.shopapi.auth;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RefreshTokenCookieFactory {

    @Getter
    @Value("${app.refresh-token.cookie-name}")
    private String cookieName;

    @Value("${app.refresh-token.expiration-ms}")
    private long expirationMs;

    public ResponseCookie create(String rawToken) {
        return ResponseCookie.from(cookieName, rawToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(Duration.ofMillis(expirationMs))
                .build();
    }

    public ResponseCookie createExpired() {
        return ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/v1/auth")
                .maxAge(0)
                .build();
    }

}
