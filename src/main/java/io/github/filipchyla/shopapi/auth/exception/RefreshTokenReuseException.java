package io.github.filipchyla.shopapi.auth.exception;

import io.github.filipchyla.shopapi.shared.exception.UnauthorizedException;

public class RefreshTokenReuseException extends UnauthorizedException {
    public RefreshTokenReuseException(String message) {
        super(message);
    }
}
