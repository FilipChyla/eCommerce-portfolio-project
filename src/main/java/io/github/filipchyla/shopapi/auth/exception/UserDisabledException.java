package io.github.filipchyla.shopapi.auth.exception;

import io.github.filipchyla.shopapi.exception.base.UnauthorizedException;

public class UserDisabledException extends UnauthorizedException {
    public UserDisabledException(String message) {
        super(message);
    }
}
