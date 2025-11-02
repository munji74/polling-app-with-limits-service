package com.microservices.apigateway.config;

import com.microservices.apigateway.filter.AuthFilter;
import com.microservices.apigateway.filter.RateLimitFilter;
import com.microservices.apigateway.filter.ValidationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfiguration {

    // Make the downstream base URL configurable per environment
    @Value("${services.user-service-base-url:http://localhost:8000}")
    private String userServiceBaseUrl;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, AuthFilter authFilter, ValidationFilter validationFilter, RateLimitFilter rateLimitFilter) {
        return builder.routes()

                // Public auth endpoints -> user-service (apply validation filter)
                .route("user-auth", r -> r.path("/auth/**")
                        .filters(f -> f.filter(validationFilter.apply(new ValidationFilter.Config())))
                        // dev: route directly to local user-service (configurable)
                        .uri(userServiceBaseUrl))

                .route("user-me", r -> r.path("/api/auth/me")
                        .filters(f -> f.rewritePath("/api/auth/me", "/user/me")
                                .filter(authFilter.apply(new AuthFilter.Config())))
                        .uri(userServiceBaseUrl))

                // Everything else to user-service (WITH auth filter + rate limit)
                .route("user-protected", r -> r.path("/api/users/**")
                        .filters(f -> f.rewritePath("/api/users/(?<remaining>.*)", "/user/${remaining}")
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config()))
                                .filter(authFilter.apply(new AuthFilter.Config())))
                        .uri(userServiceBaseUrl))

                // Example: poll-service protected routes (apply rate limiting and auth)
                .route("poll-protected", r -> r.path("/api/polls/**")
                        .filters(f -> f.filter(rateLimitFilter.apply(new RateLimitFilter.Config()))
                                .filter(authFilter.apply(new AuthFilter.Config())))
                        .uri("lb://poll-service"))

                // Note: Do not proxy /actuator through the gateway routes (avoid self-proxy/loop).
                // The gateway exposes its own actuator endpoints on its server port and they will be handled directly.

                 .build();
     }
 }
