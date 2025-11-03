package com.microservices.apigateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.apigateway.config.RouteValidator;
import com.microservices.apigateway.util.JwtUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    public static class Config { /* empty */ }

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    @Autowired
    public AuthFilter(RouteValidator validator, JwtUtil jwtUtil) {
        super(Config.class);
        this.validator = validator;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var request = exchange.getRequest();

            if (validator.isSecured.test(request)) {
                String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (auth == null || !auth.startsWith("Bearer ")) {
                    log.warn("Missing or invalid Authorization header for request: {}", request.getURI());
                    return unauthorizedJson(exchange, "Missing or invalid Authorization header");
                }
                String token = auth.substring(7);

                try {
                    Claims claims = jwtUtil.validateAndGetClaims(token);
                    String sub   = claims.getSubject();            // user id
                    String email = (String) claims.get("email");
                    String roles = (String) claims.get("roles");

                    var mutated = request.mutate()
                            .header("X-User-Id", sub == null ? "" : sub)
                            .header("X-User-Email", email == null ? "" : email)
                            .header("X-User-Roles", roles == null ? "" : roles)
                            .build();

                    return chain.filter(exchange.mutate().request(mutated).build());

                } catch (ExpiredJwtException eje) {
                    log.warn("Expired token for request {}: {}", request.getURI(), eje.getMessage());
                    return unauthorizedJson(exchange, "Token expired");
                } catch (SignatureException | SecurityException se) {
                    log.warn("Invalid token signature for request {}: {}", request.getURI(), se.getMessage());
                    return unauthorizedJson(exchange, "Invalid token signature");
                } catch (MalformedJwtException mje) {
                    log.warn("Malformed token for request {}: {}", request.getURI(), mje.getMessage());
                    return unauthorizedJson(exchange, "Malformed token");
                } catch (UnsupportedJwtException uje) {
                    log.warn("Unsupported token for request {}: {}", request.getURI(), uje.getMessage());
                    return unauthorizedJson(exchange, "Unsupported token");
                } catch (IllegalArgumentException iae) {
                    log.warn("Illegal argument while parsing token for request {}: {}", request.getURI(), iae.getMessage());
                    return unauthorizedJson(exchange, "Invalid token");
                } catch (JwtException je) {
                    log.warn("JWT error for request {}: {}", request.getURI(), je.getMessage());
                    return unauthorizedJson(exchange, "Invalid token");
                } catch (Exception e) {
                    log.warn("Token validation failed for request {}: {}", request.getURI(), e.getMessage());
                    return unauthorizedJson(exchange, "Invalid or expired token");
                }
            } else {
                // Open endpoint: if Authorization header is present, try to enrich with identity, but never block.
                String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (auth != null && auth.startsWith("Bearer ")) {
                    String token = auth.substring(7);
                    try {
                        Claims claims = jwtUtil.validateAndGetClaims(token);
                        String sub   = claims.getSubject();
                        String email = (String) claims.get("email");
                        String roles = (String) claims.get("roles");
                        var mutated = request.mutate()
                                .header("X-User-Id", sub == null ? "" : sub)
                                .header("X-User-Email", email == null ? "" : email)
                                .header("X-User-Roles", roles == null ? "" : roles)
                                .build();
                        return chain.filter(exchange.mutate().request(mutated).build());
                    } catch (Exception e) {
                        // don't fail open endpoints due to token issues; just continue without identity
                        log.debug("Ignoring invalid token on open endpoint {}: {}", request.getURI(), e.getMessage());
                    }
                }
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> unauthorizedJson(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        Map<String,Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", message);
        byte[] bytes;
        try {
            bytes = mapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"message\":\""+message+"\"}").getBytes(StandardCharsets.UTF_8);
        }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }
}
