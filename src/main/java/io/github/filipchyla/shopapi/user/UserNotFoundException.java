package io.github.filipchyla.shopapi.user;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super(username);
    }
}
