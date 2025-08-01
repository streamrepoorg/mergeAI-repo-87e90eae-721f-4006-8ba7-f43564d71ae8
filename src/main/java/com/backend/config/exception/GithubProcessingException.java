package com.backend.config.exception;

public class GithubProcessingException extends RuntimeException {
    public GithubProcessingException(String message) {
        super(message);
    }
}
