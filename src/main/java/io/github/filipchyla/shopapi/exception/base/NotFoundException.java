package io.github.filipchyla.shopapi.exception.base;

public abstract class NotFoundException extends RuntimeException {
    protected NotFoundException(String message) {
        super(message);
    }
}
