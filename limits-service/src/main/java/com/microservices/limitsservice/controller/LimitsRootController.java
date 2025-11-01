package com.microservices.limitsservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import com.microservices.limitsservice.service.RateLimiterService;

@RestController
public class LimitsRootController {

    private final RateLimiterService limiter;

    @Autowired
    public LimitsRootController(RateLimiterService limiter) {
        this.limiter = limiter;
    }

    @PostMapping(path = "/limits/check")
    public Map<String, Object> check(@RequestBody Map<String, Object> req) {
        String key = req.getOrDefault("key", "").toString();
        String route = req.getOrDefault("route", "").toString();
        int weight = 1;
        try {
            Object w = req.get("weight");
            if (w != null) weight = Integer.parseInt(w.toString());
        } catch (Exception ignored) {}

        // Determine rate parameters based on route
        int replenish = route.startsWith("/api/") ? 10 : 20;
        int burst = route.startsWith("/api/") ? 20 : 40;
        if (key.startsWith("user:")) {
            replenish = route.startsWith("/api/") ? 30 : 20;
            burst = route.startsWith("/api/") ? 60 : 40;
        }

        var result = limiter.tryConsume(key, route, weight, replenish, burst);
        return Map.of(
                "allowed", result.allowed(),
                "remaining", result.remaining(),
                "limit", result.limit(),
                "resetSeconds", result.resetSeconds()
        );
    }
}

