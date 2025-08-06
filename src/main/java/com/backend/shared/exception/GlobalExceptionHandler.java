package com.backend.shared.exception;

import com.backend.dto.response.ResponseDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GithubNotFoundException.class)
    public ResponseEntity<ResponseDetails> handleGithubNotFoundException(GithubNotFoundException ex) {
        log.error("Not found error: {}", ex.getMessage(), ex);
        ResponseDetails error = new ResponseDetails(LocalDateTime.now(), ex.getMessage(), HttpStatus.NOT_FOUND.toString(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        if (ex.getMessage().contains("favicon.ico")) {
            return ResponseEntity.notFound().build();
        }
        ResponseDetails responseDetails = new ResponseDetails(LocalDateTime.now(), "Resource not found: " + ex.getMessage(), HttpStatus.NOT_FOUND.toString(), request.getDescription(false));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseDetails);
    }

    @ExceptionHandler(GithubProcessingException.class)
    public ResponseEntity<ResponseDetails> handleGithubProcessingException(GithubProcessingException ex) {
        log.error("Processing error: {}", ex.getMessage(), ex);
        ResponseDetails error = new ResponseDetails(LocalDateTime.now(), ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDetails> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage(), ex);
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((msg1, msg2) -> msg1 + "; " + msg2)
                .orElse("Invalid request data");
        ResponseDetails error = new ResponseDetails(LocalDateTime.now(), errorMessage, HttpStatus.BAD_REQUEST.toString(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDetails> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        ResponseDetails error = new ResponseDetails(LocalDateTime.now(), "An unexpected error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.toString(), null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}