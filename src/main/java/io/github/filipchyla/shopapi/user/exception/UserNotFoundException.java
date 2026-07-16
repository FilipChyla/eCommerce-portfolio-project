package io.github.filipchyla.shopapi.user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super(username);
    }
}
