package com.microservices.apigateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Hardened RateLimitFilter delegates rate checks to the external limits-service.
 * It supports timeout, a circuit breaker, local caching, metrics and configurable fail-open policy.
 */
@Component
public class RateLimitFilter extends AbstractGatewayFilterFactory<RateLimitFilter.Config> {

    public static class Config { }

    private final WebClient webClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final Cache<String, JsonNode> cache;
    private final CircuitBreaker circuitBreaker;
    private final boolean failOpen;
    private final Timer latencyTimer;

    // Micrometer counters
    private final Counter allowedCounter;
    private final Counter deniedCounter;
    private final Counter errorCounter;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;

    public RateLimitFilter(
            @Value("${limits.service.url:http://localhost:8082}") String limitsServiceUrl,
            @Value("${limits.service.timeout-ms:250}") long timeoutMs,
            @Value("${limits.service.fail-open:true}") boolean failOpen,
            @Value("${limits.service.cache-ttl-ms:500}") long cacheTtlMs,
            MeterRegistry meterRegistry
    ) {
        super(Config.class);
        this.failOpen = failOpen;

        // Reactor Netty HttpClient with response timeout
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        this.webClient = WebClient.builder()
                .baseUrl(limitsServiceUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        // Small local cache for allowed responses
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMillis(cacheTtlMs))
                .maximumSize(10_000)
                .build();

        // Circuit breaker config
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(5)
                .slidingWindowSize(20)
                .build();
        this.circuitBreaker = CircuitBreaker.of("limitsService", cbConfig);

        this.latencyTimer = meterRegistry.timer("limits.request.latency");

        // meters
        this.allowedCounter = meterRegistry.counter("limits.requests.allowed");
        this.deniedCounter = meterRegistry.counter("limits.requests.denied");
        this.errorCounter = meterRegistry.counter("limits.requests.error");
        this.cacheHitCounter = meterRegistry.counter("limits.cache.hits");
        this.cacheMissCounter = meterRegistry.counter("limits.cache.misses");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            var req = exchange.getRequest();
            var path = req.getURI().getPath();

            String userId = req.getHeaders().getFirst("X-User-Id");
            String key = (userId != null && !userId.isBlank()) ? ("user:" + userId) : ("ip:" + exchange.getRequest().getRemoteAddress());
            String cacheKey = key + "::" + path;

            // check cache
            JsonNode cached = cache.getIfPresent(cacheKey);
            if (cached != null) {
                cacheHitCounter.increment();
                boolean allowed = cached.path("allowed").asBoolean(true);
                int remaining = cached.path("remaining").asInt(-1);
                int limit = cached.path("limit").asInt(-1);
                int reset = cached.path("resetSeconds").asInt(-1);
                if (!allowed) {
                    deniedCounter.increment();
                    log.warn("Rate limit (cached) exceeded for {} on {}", key, path);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    Map<String,Object> resp = Map.of("success", false, "message", "Rate limit exceeded");
                    byte[] bytes;
                    try { bytes = mapper.writeValueAsString(resp).getBytes(StandardCharsets.UTF_8); } catch (Exception e) { bytes = ("{\"success\":false,\"message\":\"Rate limit exceeded\"}").getBytes(StandardCharsets.UTF_8); }
                    if (remaining >= 0) exchange.getResponse().getHeaders().add("X-Rate-Remaining", String.valueOf(remaining));
                    if (limit >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", String.valueOf(limit));
                    if (reset >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Reset", String.valueOf(reset));
                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                }
                if (remaining >= 0) exchange.getResponse().getHeaders().add("X-Rate-Remaining", String.valueOf(remaining));
                if (limit >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", String.valueOf(limit));
                if (reset >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Reset", String.valueOf(reset));
                return chain.filter(exchange);
            }
            cacheMissCounter.increment();

            Map<String,String> body = new HashMap<>();
            body.put("key", key);
            body.put("route", path);

            // Build the reactive call and decorate with circuit breaker operator
            Mono<JsonNode> call = webClient.post()
                    .uri("/limits/check")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));

            long start = System.nanoTime();
            return call.flatMap(json -> {
                latencyTimer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
                boolean allowed = json.path("allowed").asBoolean(true);
                int remaining = json.path("remaining").asInt(-1);
                int limit = json.path("limit").asInt(-1);
                int reset = json.path("resetSeconds").asInt(-1);
                if (!allowed) {
                    // cache deny and respond 429
                    cache.put(cacheKey, json);
                    deniedCounter.increment();
                    log.warn("Rate limit exceeded for key {} on route {}", key, path);
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    Map<String,Object> resp = Map.of("success", false, "message", "Rate limit exceeded");
                    byte[] bytes;
                    try { bytes = mapper.writeValueAsString(resp).getBytes(StandardCharsets.UTF_8); } catch (Exception e) { bytes = ("{\"success\":false,\"message\":\"Rate limit exceeded\"}").getBytes(StandardCharsets.UTF_8); }
                    if (remaining >= 0) exchange.getResponse().getHeaders().add("X-Rate-Remaining", String.valueOf(remaining));
                    if (limit >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", String.valueOf(limit));
                    if (reset >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Reset", String.valueOf(reset));
                    return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
                }
                allowedCounter.increment();
                if (remaining >= 0) exchange.getResponse().getHeaders().add("X-Rate-Remaining", String.valueOf(remaining));
                if (limit >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Limit", String.valueOf(limit));
                if (reset >= 0) exchange.getResponse().getHeaders().add("X-Rate-Limit-Reset", String.valueOf(reset));
                // cache positive response (so repeated calls in TTL don't hit limits-service)
                cache.put(cacheKey, json);
                return chain.filter(exchange);
            }).onErrorResume(ex -> {
                // metrics increment could be added here
                errorCounter.increment();
                log.error("Limits service check failed: {}", ex.toString());
                if (failOpen) {
                    log.warn("Fail-open enabled: allowing request despite limits-service failure");
                    return chain.filter(exchange);
                }
                // fail-closed: return 503 or 429; we'll return 503 Service Unavailable
                exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
                Map<String,Object> resp = Map.of("success", false, "message", "Limits service unavailable");
                byte[] bytes;
                try { bytes = mapper.writeValueAsString(resp).getBytes(StandardCharsets.UTF_8); } catch (Exception e) { bytes = ("{\"success\":false,\"message\":\"Limits service unavailable\"}").getBytes(StandardCharsets.UTF_8); }
                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            });
        };
    }
}
