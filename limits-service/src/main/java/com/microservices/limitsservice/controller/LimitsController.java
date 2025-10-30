package com.microservices.limitsservice.controller;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/limits")
public class LimitsController {

    record Rate(int replenishRate, int burstCapacity) {}
    record RouteLimits(Rate anonymous, Rate authenticated) {}

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
}
