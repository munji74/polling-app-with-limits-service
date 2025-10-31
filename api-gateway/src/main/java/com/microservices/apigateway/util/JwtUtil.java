package com.microservices.apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final String expectedIssuer;
    private final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String expectedIssuer
    ) {
        byte[] secretBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);

        // Ensure key is at least 256 bits (32 bytes) for HMAC-SHA
        if (secretBytes.length < 32) {
            log.warn("Configured JWT secret is shorter than 32 bytes; deriving a 256-bit key from the provided secret. Please set a secure key of at least 32 bytes in configuration.");
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                byte[] hashed = sha256.digest(secretBytes);
                // use first 32 bytes (full sha256)
                secretBytes = Arrays.copyOf(hashed, 32);
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to derive JWT secret key", ex);
            }
        }

        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expectedIssuer = expectedIssuer;
    }

    public Claims validateAndGetClaims(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);

        Claims claims = jws.getBody();
        if (expectedIssuer != null && !expectedIssuer.equals(claims.getIssuer())) {
            throw new JwtException("Invalid issuer");
        }
        return claims;
    }
}
