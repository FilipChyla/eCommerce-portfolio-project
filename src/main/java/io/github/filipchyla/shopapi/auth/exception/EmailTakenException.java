package io.github.filipchyla.shopapi.auth.exception;

public class EmailTakenException extends RuntimeException {
    public EmailTakenException(String message) {
        super(message);
    }
}
