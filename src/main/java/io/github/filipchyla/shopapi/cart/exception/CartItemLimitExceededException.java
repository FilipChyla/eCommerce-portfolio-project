package io.github.filipchyla.shopapi.cart.exception;

import io.github.filipchyla.shopapi.shared.exception.BadRequestException;

public class CartItemLimitExceededException extends BadRequestException {
    public CartItemLimitExceededException(String message) {
        super(message);
    }
}
