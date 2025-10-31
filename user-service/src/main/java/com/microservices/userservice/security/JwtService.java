package com.microservices.userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final String secret;
    private final String issuer;
    private final long accessExpSeconds;
    private SecretKey key;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.issuer}") String issuer,
                      @Value("${security.jwt.access-exp-seconds}") long accessExpSeconds) {
        this.secret = secret;
        this.issuer = issuer;
        this.accessExpSeconds = accessExpSeconds;
    }

    @PostConstruct
    private void init() {
        if (secret == null || secret.isBlank() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.jwt.secret must be provided and at least 32 bytes long (set in config server or env). Please set JWT_SECRET env or configure in config-repo.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private SecretKey key() {
        return key;
    }

    public String mintAccessToken(String subject, String email, String rolesCsv) {
        var now = Instant.now();
        var exp = now.plusSeconds(accessExpSeconds);
        return Jwts.builder()
                .setSubject(subject)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of("email", email, "roles", rolesCsv))
                .signWith(key(), SignatureAlgorithm.HS512)
                .compact();
    }

    public long getAccessExpSeconds() { return accessExpSeconds; }
}
