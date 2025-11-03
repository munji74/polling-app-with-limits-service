package com.microservices.apigateway.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
public class DebugController {

    @PostMapping(value = "/echo", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> echo(@RequestHeader Map<String, String> headers,
                                                            @RequestBody(required = false) Mono<String> bodyMono) {
        return (bodyMono == null ? Mono.just("") : bodyMono.defaultIfEmpty(""))
                .map(body -> {
                    Map<String, Object> out = new HashMap<>();
                    out.put("headers", headers);
                    out.put("body", body == null ? "" : body);
                    return ResponseEntity.ok(out);
                });
    }
}
