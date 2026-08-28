package io.github.filipchyla.shopapi.role;

import io.github.filipchyla.shopapi.shared.exception.NotFoundException;

public class RoleNotFoundException extends NotFoundException {
    public RoleNotFoundException(String message) {
        super(message);
    }
}
