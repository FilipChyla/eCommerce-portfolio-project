package io.github.filipchyla.shopapi.shared.exception;

public abstract class UnauthorizedException extends RuntimeException {
    protected UnauthorizedException(String message) {
        super(message);
    }
}
