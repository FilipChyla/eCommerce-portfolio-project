package io.github.filipchyla.shopapi.cart;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CartTokenCookieFactory {

    private final String cartTokenCookieName;
    private final Duration cartTokenMaxAge;
    private final boolean secure;

    public CartTokenCookieFactory(
            @Value("${app.cart-token.cookie-name}") String cartTokenCookieName,
            @Value("${app.cart-token.expiration-ms}") long expirationMs,
            @Value("${app.cart-token.secure}") boolean secure) {
        this.cartTokenCookieName = cartTokenCookieName;
        this.cartTokenMaxAge = Duration.ofMillis(expirationMs);
        this.secure = secure;
    }

    public ResponseCookie create(String cartToken) {
        if (cartToken == null) return null;
        return ResponseCookie.from(cartTokenCookieName, cartToken)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(cartTokenMaxAge)
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie.from(cartTokenCookieName, "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();
    }
}