package io.github.filipchyla.shopapi.product.exception;

import io.github.filipchyla.shopapi.shared.exception.NotFoundException;

public class ProductNotFoundException extends NotFoundException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
