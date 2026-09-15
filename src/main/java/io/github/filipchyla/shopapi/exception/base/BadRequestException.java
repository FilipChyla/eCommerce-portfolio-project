package io.github.filipchyla.shopapi.exception.base;

public abstract class BadRequestException extends RuntimeException {
    protected BadRequestException(String message) {
        super(message);
    }
}
