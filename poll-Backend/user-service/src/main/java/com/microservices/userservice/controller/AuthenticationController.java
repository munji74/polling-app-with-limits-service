package com.microservices.userservice.controller;

import com.microservices.userservice.payload.AuthResponse;
import com.microservices.userservice.payload.LoginRequest;
import com.microservices.userservice.payload.RegisterRequest;
import com.microservices.userservice.service.AuthAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthAppService auth;

    public AuthenticationController(AuthAppService auth) {
        this.auth = auth;
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        auth.register(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }
}
