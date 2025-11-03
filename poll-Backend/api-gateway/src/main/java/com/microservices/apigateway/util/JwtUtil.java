package com.microservices.apigateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final String expectedIssuer;
    private final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    public JwtUtil(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.issuer}") String expectedIssuer,
            @Value("${security.jwt.secret-base64:true}") boolean secretIsBase64
    ) {
        byte[] keyMaterial;
        if (secret == null) {
            keyMaterial = new byte[0];
        } else if (secretIsBase64) {
            keyMaterial = Decoders.BASE64.decode(secret);
        } else {
            keyMaterial = secret.getBytes(StandardCharsets.UTF_8);
        }

        // For HS512 need >= 64 bytes
        if (keyMaterial.length < 64) {
            throw new IllegalStateException("Configured JWT secret material must be at least 64 bytes for HS512. Provide a secure key of 64+ bytes (consider Base64). Current length=" + keyMaterial.length);
        }

        this.key = Keys.hmacShaKeyFor(keyMaterial);
        this.expectedIssuer = expectedIssuer;

        try {
            int rawLen = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
            log.info("JwtUtil initialized (secret material length={} bytes, base64={}, issuer={})", keyMaterial.length, secretIsBase64, this.expectedIssuer);
        } catch (Exception e) {
            log.info("JwtUtil initialized (issuer={})", this.expectedIssuer);
        }
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
