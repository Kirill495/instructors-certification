package org.tourism.instructors.application.user.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(int id) {
        super("User not found: " + id);
    }
    public UserNotFoundException(String name) {
        super("User not found: " + name);
    }
}
