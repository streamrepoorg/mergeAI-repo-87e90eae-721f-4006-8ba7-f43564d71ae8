package com.backend.config.exception;

public class UserNotVerified extends RuntimeException{

    public UserNotVerified(String message) {
        super(message);
    }
}
