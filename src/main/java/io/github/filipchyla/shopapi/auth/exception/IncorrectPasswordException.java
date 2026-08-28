package io.github.filipchyla.shopapi.auth.exception;

import io.github.filipchyla.shopapi.shared.exception.UnauthorizedException;

public class IncorrectPasswordException extends UnauthorizedException {
    public IncorrectPasswordException(String message) {
        super(message);
    }
}
