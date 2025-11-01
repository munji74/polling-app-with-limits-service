package com.microservices.limitsservice.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import com.microservices.limitsservice.service.RateLimiterService;

@RestController
@RequestMapping("/api/limits")
public class LimitsController {

    record Rate(int replenishRate, int burstCapacity) {}
    record RouteLimits(Rate anonymous, Rate authenticated) {}

    private final RateLimiterService limiter;

    @Autowired
    public LimitsController(RateLimiterService limiter) {
        this.limiter = limiter;
    }

    @GetMapping("/route")
    public RouteLimits route(@RequestParam String path) {
        if (path.startsWith("/api/")) {
            return new RouteLimits(new Rate(10, 20), new Rate(30, 60));
        }
        return new RouteLimits(new Rate(20, 40), new Rate(20, 40));
    }

    @GetMapping("/features")
    public Map<String, Object> features() {
        return Map.of("voting.enabled", true, "createPoll.enabled", true);
    }

    // New endpoint: POST /api/limits/limits/check (keeps compatibility)
    // Also expose POST /limits/check at root so gateway can call it on base URL
    @PostMapping(path = {"/limits/check", "/limits/check"})
    public Map<String, Object> check(@RequestBody Map<String, Object> req) {
        String key = req.getOrDefault("key", "").toString();
        String route = req.getOrDefault("route", "").toString();
        int weight = 1;
        try {
            Object w = req.get("weight");
            if (w != null) weight = Integer.parseInt(w.toString());
        } catch (Exception ignored) {}

        // Determine rate parameters based on route
        Rate rate = route.startsWith("/api/") ? new Rate(10, 20) : new Rate(20, 40);
        // If authenticated user key
        if (key.startsWith("user:")) {
            rate = route.startsWith("/api/") ? new Rate(30, 60) : new Rate(20, 40);
        }

        var result = limiter.tryConsume(key, route, weight, rate.replenishRate, rate.burstCapacity);
        return Map.of(
                "allowed", result.allowed(),
                "remaining", result.remaining(),
                "limit", result.limit(),
                "resetSeconds", result.resetSeconds()
        );
    }
}
