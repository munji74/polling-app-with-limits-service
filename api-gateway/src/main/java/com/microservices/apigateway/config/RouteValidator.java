package com.microservices.apigateway.config;

import org.springframework.http.HttpMethod;
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
        final HttpMethod httpMethod = request.getMethod();
        final String method = httpMethod != null ? httpMethod.name() : "";

        // Public read-only access to polls (frontend shows polls to anonymous users)
        if (HttpMethod.GET.name().equalsIgnoreCase(method) && path.startsWith("/api/polls")) {
            return false; // open
        }

        // Open actuator & auth endpoints
        boolean isOpen = OPEN_API_ENDPOINTS.stream().anyMatch(pattern -> {
            if (pattern.endsWith("/")) {
                return path.startsWith(pattern);
            }
            return path.equals(pattern);
        });
        return !isOpen;
    };
}
