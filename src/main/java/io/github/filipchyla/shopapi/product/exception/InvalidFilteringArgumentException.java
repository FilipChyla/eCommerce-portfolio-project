package io.github.filipchyla.shopapi.product.exception;

import io.github.filipchyla.shopapi.shared.exception.BadRequestException;

public class InvalidFilteringArgumentException extends BadRequestException {
    public InvalidFilteringArgumentException(String message) {
        super(message);
    }
}
