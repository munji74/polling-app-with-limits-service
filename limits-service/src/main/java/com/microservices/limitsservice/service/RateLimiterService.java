package com.microservices.limitsservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    public record ConsumeResult(boolean allowed, int remaining, int limit, int resetSeconds) {}

    static class TokenBucket {
        final int capacity;
        final int refillPerSecond;
        double tokens;
        Instant lastRefill;

        TokenBucket(int refillPerSecond, int capacity) {
            this.refillPerSecond = refillPerSecond;
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefill = Instant.now();
        }

        synchronized ConsumeResult tryConsume(int weight) {
            refill();
            if (tokens >= weight) {
                tokens -= weight;
                int remaining = (int)Math.floor(tokens);
                return new ConsumeResult(true, remaining, capacity, 60);
            }
            return new ConsumeResult(false, (int)Math.floor(tokens), capacity, 60);
        }

        private void refill() {
            Instant now = Instant.now();
            double seconds = (now.toEpochMilli() - lastRefill.toEpochMilli()) / 1000.0;
            if (seconds <= 0) return;
            double refill = seconds * refillPerSecond;
            tokens = Math.min(capacity, tokens + refill);
            lastRefill = now;
        }
    }

    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public RateLimiterService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public ConsumeResult tryConsume(String key, String route, int weight, int replenishRate, int burstCapacity) {
        String cacheKey = Objects.toString(key, "") + "::" + Objects.toString(route, "");
        TokenBucket bucket = buckets.computeIfAbsent(cacheKey, k -> new TokenBucket(replenishRate, burstCapacity));

        long start = System.nanoTime();
        ConsumeResult result = bucket.tryConsume(weight);
        long duration = System.nanoTime() - start;

        // Low-cardinality tags
        String userType = key != null && key.startsWith("user:") ? "user" : "anon";
        String routeGroup = route != null && route.startsWith("/api/") ? "api" : "other";

        // Timers for latency
        Timer.builder("limits.request")
                .description("Latency of limits checks")
                .tag("routeGroup", routeGroup)
                .tag("userType", userType)
                .tag("allowed", String.valueOf(result.allowed()))
                .register(meterRegistry)
                .record(duration, TimeUnit.NANOSECONDS);

        // Counters for decisions
        if (result.allowed()) {
            Counter.builder("limits.requests.allowed")
                    .description("Count of allowed requests by limits service")
                    .tag("routeGroup", routeGroup)
                    .tag("userType", userType)
                    .register(meterRegistry)
                    .increment();
        } else {
            Counter.builder("limits.requests.denied")
                    .description("Count of denied requests by limits service")
                    .tag("routeGroup", routeGroup)
                    .tag("userType", userType)
                    .register(meterRegistry)
                    .increment();
        }

        return result;
    }
}
