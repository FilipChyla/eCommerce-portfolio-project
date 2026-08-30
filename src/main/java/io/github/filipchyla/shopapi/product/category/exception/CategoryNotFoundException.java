package io.github.filipchyla.shopapi.product.category.exception;

import io.github.filipchyla.shopapi.shared.exception.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
