package com.microservices.userservice.payload;

public record LoginRequest(
        String email,
        String password
) {}
