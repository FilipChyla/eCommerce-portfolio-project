package io.github.filipchyla.shopapi.product.exception;

import io.github.filipchyla.shopapi.exception.base.NotFoundException;

public class ProductNotFoundException extends NotFoundException {
    public ProductNotFoundException(String message) {
        super(message);
    }
}
