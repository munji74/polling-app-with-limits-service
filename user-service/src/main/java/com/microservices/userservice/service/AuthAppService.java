package com.microservices.userservice.service;

import com.microservices.userservice.model.User;
import com.microservices.userservice.payload.AuthResponse;
import com.microservices.userservice.payload.LoginRequest;
import com.microservices.userservice.payload.RegisterRequest;
import com.microservices.userservice.repository.UserRepository;
import com.microservices.userservice.security.JwtService;
import com.microservices.userservice.util.enums.Role;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthAppService {
    private final UserRepository users;
    private final JwtService jwt;
    private final BCryptPasswordEncoder encoder;

    public AuthAppService(UserRepository users, JwtService jwt, BCryptPasswordEncoder encoder) {
        this.users = users;
        this.jwt = jwt;
        this.encoder = encoder;
    }

    @Transactional
    public void register(RegisterRequest req) {
        if (users.existsByEmail(req.email())) throw new IllegalArgumentException("Email already in use");

        var user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));

        // assign default role USER
        user.setRoles(Set.of(Role.USER));

        users.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        var user = users.findByEmail(req.email())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        // Build CSV from Set<Role> (USER,ADMIN,...)
        var rolesCsv = user.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));

        var token = jwt.mintAccessToken(String.valueOf(user.getId()), user.getEmail(), rolesCsv);
        return new AuthResponse(token, "Bearer", jwt.getAccessExpSeconds());
    }
}
