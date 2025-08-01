package com.backend.config.exception;

public class GithubNotFoundException extends RuntimeException {
    public GithubNotFoundException(String message) {
        super(message);
    }
}