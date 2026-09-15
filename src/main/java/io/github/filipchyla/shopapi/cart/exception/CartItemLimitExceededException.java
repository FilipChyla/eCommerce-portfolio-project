package io.github.filipchyla.shopapi.cart.exception;

import io.github.filipchyla.shopapi.exception.base.BadRequestException;

public class CartItemLimitExceededException extends BadRequestException {
    public CartItemLimitExceededException(String message) {
        super(message);
    }
}
