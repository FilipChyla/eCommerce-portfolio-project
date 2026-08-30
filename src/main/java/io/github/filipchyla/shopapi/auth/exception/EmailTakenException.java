package io.github.filipchyla.shopapi.auth.exception;

import io.github.filipchyla.shopapi.shared.exception.ConflictException;

public class EmailTakenException extends ConflictException {
    public EmailTakenException(String message) {
        super(message);
    }
}
