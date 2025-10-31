package com.microservices.apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final String expectedIssuer;

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String expectedIssuer
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expectedIssuer = expectedIssuer;
    }

    public Claims validateAndGetClaims(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

        Claims claims = jws.getBody();
        if (!expectedIssuer.equals(claims.getIssuer())) {
            throw new JwtException("Invalid issuer");
        }
        return claims;
    }
}
