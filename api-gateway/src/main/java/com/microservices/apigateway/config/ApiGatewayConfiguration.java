package com.microservices.apigateway.config;

import com.microservices.apigateway.filter.AuthFilter;
import com.microservices.apigateway.filter.RateLimitFilter;
import com.microservices.apigateway.filter.ValidationFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfiguration {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, AuthFilter authFilter, ValidationFilter validationFilter, RateLimitFilter rateLimitFilter) {
        return builder.routes()

                // Public auth endpoints -> user-service (apply validation filter)
                .route("user-auth", r -> r.path("/auth/**")
                        .filters(f -> f.filter(validationFilter.apply(new ValidationFilter.Config())))
                        // dev: route directly to local user-service
                        .uri("http://localhost:8081"))

                // Everything else to user-service (WITH auth filter + rate limit)
                .route("user-protected", r -> r.path("/api/users/**")
                        .filters(f -> f.rewritePath("/api/users/(?<remaining>.*)", "/user/${remaining}")
                                .filter(rateLimitFilter.apply(new RateLimitFilter.Config()))
                                .filter(authFilter.apply(new AuthFilter.Config())))
                        .uri("http://localhost:8081"))

                // Example: poll-service protected routes (apply rate limiting and auth)
                .route("poll-protected", r -> r.path("/api/polls/**")
                        .filters(f -> f.filter(rateLimitFilter.apply(new RateLimitFilter.Config()))
                                .filter(authFilter.apply(new AuthFilter.Config())))
                        .uri("lb://poll-service"))

                // Actuator passthrough (no filter)
                .route("actuator", r -> r.path("/actuator/**")
                        .uri("lb://api-gateway"))

                .build();
    }
}
