package com.backend.shared.exception;

public class UserNotVerified extends RuntimeException{

    public UserNotVerified(String message) {
        super(message);
    }
}
