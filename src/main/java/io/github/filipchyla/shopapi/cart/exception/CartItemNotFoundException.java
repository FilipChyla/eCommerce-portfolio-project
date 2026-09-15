package io.github.filipchyla.shopapi.cart.exception;

import io.github.filipchyla.shopapi.shared.exception.NotFoundException;

public class CartItemNotFoundException extends NotFoundException {
    public CartItemNotFoundException(String message) {
        super(message);
    }
}
