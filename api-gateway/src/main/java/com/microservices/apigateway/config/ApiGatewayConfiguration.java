package com.microservices.apigateway.config;

import com.microservices.apigateway.filter.AuthFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiGatewayConfiguration {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, AuthFilter authFilter) {
        return builder.routes()

                // Public auth endpoints -> user-service (NO auth filter)
                .route("user-auth", r -> r.path("/auth/**")
                        .uri("lb://user-service"))

                // Everything else to user-service (WITH auth filter)
                .route("user-protected", r -> r.path("/api/users/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthFilter.Config())))
                        .uri("lb://user-service"))

                // Example: poll-service protected routes (if you have it)
                .route("poll-protected", r -> r.path("/api/polls/**")
                        .filters(f -> f.filter(authFilter.apply(new AuthFilter.Config())))
                        .uri("lb://poll-service"))

                // Actuator passthrough (no filter)
                .route("actuator", r -> r.path("/actuator/**")
                        .uri("lb://api-gateway"))

                .build();
    }
}
