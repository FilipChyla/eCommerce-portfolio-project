package io.github.filipchyla.shopapi.product.exception;

import org.springframework.security.authentication.BadCredentialsException;

public class InvalidStockQuantityException extends BadCredentialsException {
    public InvalidStockQuantityException(String message) {
        super(message);
    }
}
