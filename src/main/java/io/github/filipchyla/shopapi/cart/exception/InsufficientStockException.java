package io.github.filipchyla.shopapi.cart.exception;

import io.github.filipchyla.shopapi.shared.exception.ConflictException;

public class InsufficientStockException extends ConflictException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
