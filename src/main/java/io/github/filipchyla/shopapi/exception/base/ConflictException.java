package io.github.filipchyla.shopapi.exception.base;

public abstract class ConflictException extends RuntimeException{
    protected ConflictException(String message) {
        super(message);
    }
}
