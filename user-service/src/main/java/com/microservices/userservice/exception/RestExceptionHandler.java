package com.microservices.userservice.exception;

import com.microservices.userservice.payload.general.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() == null ? "Bad request" : ex.getMessage();
        if (msg.toLowerCase().contains("email already in use")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse(false, msg));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse(false, msg));
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseBody
    public ResponseEntity<ApiResponse> handleResponseStatus(ResponseStatusException ex) {
        String reason = ex.getReason() == null ? ex.getMessage() : ex.getReason();
        int status = ex.getStatusCode() == null ? HttpStatus.INTERNAL_SERVER_ERROR.value() : ex.getStatusCode().value();
        return ResponseEntity.status(status).body(new ApiResponse(false, reason));
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ApiResponse> handleAny(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, ex.getMessage()));
    }
}
