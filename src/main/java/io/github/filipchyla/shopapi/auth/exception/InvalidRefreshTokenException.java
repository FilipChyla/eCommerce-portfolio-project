package io.github.filipchyla.shopapi.auth.exception;

import io.github.filipchyla.shopapi.exception.base.UnauthorizedException;

public class InvalidRefreshTokenException extends UnauthorizedException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
