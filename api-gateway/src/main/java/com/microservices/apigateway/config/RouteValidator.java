package com.microservices.apigateway.config;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {

    // Paths that DO NOT require a token
    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/auth/sign-in",
            "/auth/sign-up",
            "/actuator",
            "/actuator/" // allow exact and prefix
    );

    // True if the path REQUIRES a token
    public Predicate<ServerHttpRequest> isSecured = request -> {
        final String path = request.getURI().getPath();
        return OPEN_API_ENDPOINTS.stream().noneMatch(pattern -> {
            if (pattern.endsWith("/")) {
                return path.startsWith(pattern);
            }
            return path.equals(pattern);
        });
    };
}
