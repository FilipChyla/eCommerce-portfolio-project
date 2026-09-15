package io.github.filipchyla.shopapi.auth.exception;

import io.github.filipchyla.shopapi.exception.base.UnauthorizedException;

public class RefreshTokenReuseException extends UnauthorizedException {
    public RefreshTokenReuseException(String message) {
        super(message);
    }
}
