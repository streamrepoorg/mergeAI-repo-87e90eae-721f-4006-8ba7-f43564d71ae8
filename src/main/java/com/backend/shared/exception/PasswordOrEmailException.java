package com.backend.shared.exception;

public class PasswordOrEmailException extends RuntimeException {
    public PasswordOrEmailException(String message, Throwable invalidPasswordLength) {
        super(message);
    }
}