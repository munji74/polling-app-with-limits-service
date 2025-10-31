package com.microservices.apigateway.filter;

import com.microservices.apigateway.config.RouteValidator;
import com.microservices.apigateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    public static class Config { /* empty */ }

    private final RouteValidator validator;
    private final JwtUtil jwtUtil;

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
                    return unauthorized(exchange);
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

                } catch (Exception e) {
                    return unauthorized(exchange);
                }
            }

            return chain.filter(exchange);
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}
