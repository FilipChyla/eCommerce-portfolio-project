package io.github.filipchyla.shopapi.cart.exception;

import io.github.filipchyla.shopapi.exception.base.ConflictException;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
