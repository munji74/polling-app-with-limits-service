package com.microservices.apigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class LoggingFilter implements GlobalFilter {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    // Toggle logging on/off
    @Value("${apigateway.logging.enabled:true}")
    private boolean enabled;

    // Comma-separated list of path prefixes to exclude from logging (defaults include actuator paths)
    @Value("${apigateway.logging.exclude-prefixes:/actuator,/actuator/metrics,/actuator/prometheus}")
    private String excludePrefixes;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (!enabled) return chain.filter(exchange);

        String path = exchange.getRequest().getPath().value();
        // skip common actuator/metrics/prometheus endpoints to reduce noisy logs
        for (String prefix : excludePrefixes.split(",")) {
            if (prefix == null || prefix.isBlank()) continue;
            if (path.startsWith(prefix.trim())) {
                return chain.filter(exchange);
            }
        }

        logger.info("[Gateway] Request path={}", path);
        return chain.filter(exchange);
    }
}
