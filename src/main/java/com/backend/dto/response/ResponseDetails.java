package com.backend.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResponseDetails {
    private LocalDateTime timestamp;
    private String message;
    private String status;
    private String path;

    public ResponseDetails(LocalDateTime timestamp, String message, String status, String path) {
        this.timestamp = timestamp;
        this.message = message;
        this.status = status;
        this.path = path;
    }
}