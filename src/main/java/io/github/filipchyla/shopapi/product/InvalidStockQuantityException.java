package io.github.filipchyla.shopapi.product;

public class InvalidStockQuantityException extends RuntimeException {
    public InvalidStockQuantityException(String message) {
        super(message);
    }
}
