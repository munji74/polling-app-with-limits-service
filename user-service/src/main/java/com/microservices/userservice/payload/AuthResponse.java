package com.microservices.userservice.payload;

public record AuthResponse(String accessToken, String tokenType, long expiresIn) {}
