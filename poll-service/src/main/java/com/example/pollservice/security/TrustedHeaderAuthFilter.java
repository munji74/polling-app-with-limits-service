package com.example.pollservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Trusts authentication performed by the API Gateway.
 * Reads X-User-Email for principal and X-User-Roles for authorities (comma-separated, e.g., "USER,ADMIN").
 * If headers are missing, the request remains unauthenticated (so public GETs still work).
 */
@Component
public class TrustedHeaderAuthFilter extends OncePerRequestFilter {

    public static final String HDR_EMAIL = "X-User-Email";
    public static final String HDR_ROLES = "X-User-Roles";

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String p = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        // public GETs on polls are allowed
        return "GET".equalsIgnoreCase(request.getMethod()) && p.startsWith("/api/polls");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String email = request.getHeader(HDR_EMAIL);
        if (email != null && !email.isBlank()) {
            String rolesHeader = request.getHeader(HDR_ROLES);
            Set<GrantedAuthority> authorities = rolesHeader == null || rolesHeader.isBlank()
                    ? Set.of()
                    : Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toSet());

            var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}
