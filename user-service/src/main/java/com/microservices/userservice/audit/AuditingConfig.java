package com.microservices.userservice.audit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Configuration
public class AuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return Optional.of(0L); // system
            var req = attrs.getRequest();
            var userId = req.getHeader("X-User-Id"); // set by Gateway after JWT validation
            if (userId == null || userId.isBlank()) return Optional.of(0L);
            try {
                return Optional.of(Long.parseLong(userId));
            } catch (NumberFormatException ex) {
                return Optional.of(0L);
            }
        };
    }
}
