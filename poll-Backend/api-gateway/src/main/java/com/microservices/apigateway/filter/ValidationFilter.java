package com.microservices.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Basic validation filter for public auth endpoints. Ensures Content-Type is JSON.
 * NOTE: we don't require Content-Length because clients may use chunked transfer encoding.
 */
@Component
public class ValidationFilter extends AbstractGatewayFilterFactory<ValidationFilter.Config> {

    public static class Config { }

    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(ValidationFilter.class);

    public ValidationFilter() { super(Config.class); }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var req = exchange.getRequest();
            var path = req.getURI().getPath();

            // Only validate auth endpoints
            if (path.startsWith("/auth/")) {
                String ct = req.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
                if (ct == null || !ct.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
                    log.warn("Rejecting request to {}: missing or non-json Content-Type: {}", path, ct);
                    return badRequest(exchange, "Content-Type must be application/json");
                }

                // Do not reject based on missing Content-Length to allow chunked requests
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> badRequest(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String,Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        byte[] bytes;
        try { bytes = mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8); }
        catch (Exception e) { bytes = ("{\"success\":false,\"message\":\""+message+"\"}").getBytes(StandardCharsets.UTF_8); }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
