package com.example.pollservice.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String,String>> handleState(IllegalStateException ex) {
        String msg = ex.getMessage();
        int code = switch (msg) {
            case "Already voted" -> 409;  // Conflict
            case "Poll expired"  -> 410;  // Gone
            default -> 409;
        };
        return ResponseEntity.status(code).body(Map.of("error", msg));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String,String>> handleArg(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
