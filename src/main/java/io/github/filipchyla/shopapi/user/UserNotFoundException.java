package io.github.filipchyla.shopapi.user;

import io.github.filipchyla.shopapi.shared.exception.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String username) {
        super(username);
    }
}
