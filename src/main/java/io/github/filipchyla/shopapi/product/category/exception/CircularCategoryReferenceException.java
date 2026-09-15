package io.github.filipchyla.shopapi.product.category.exception;

import io.github.filipchyla.shopapi.exception.base.ConflictException;

public class CircularCategoryReferenceException extends ConflictException {
    public CircularCategoryReferenceException(String message) {
        super(message);
    }
}
