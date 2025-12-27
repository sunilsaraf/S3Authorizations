package com.example.s3auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handle(Exception ex) {
        // In production avoid returning detailed error messages; return a code and correlate with logs/traces.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "timestamp", Instant.now().toString(),
                        "message", "internal_error",
                        "details", ex.getMessage()
                ));
    }
}