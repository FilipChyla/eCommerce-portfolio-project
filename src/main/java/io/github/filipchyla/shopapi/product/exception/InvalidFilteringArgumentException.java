package io.github.filipchyla.shopapi.product.exception;

import io.github.filipchyla.shopapi.exception.base.BadRequestException;

public class InvalidFilteringArgumentException extends BadRequestException {
    public InvalidFilteringArgumentException(String message) {
        super(message);
    }
}
