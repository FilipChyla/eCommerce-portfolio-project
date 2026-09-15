package io.github.filipchyla.shopapi.user;

import io.github.filipchyla.shopapi.exception.base.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String username) {
        super(username);
    }
}
