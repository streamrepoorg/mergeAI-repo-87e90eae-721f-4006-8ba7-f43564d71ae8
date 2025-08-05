package com.backend.shared.exception;

public class GithubProcessingException extends RuntimeException {
    public GithubProcessingException(String message) {
        super(message);
    }
}
