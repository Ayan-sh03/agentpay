package com.payment.payment.agent.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Configuration
public class RateLimiterConfig {

    @Bean
    @Primary
    public KeyResolver remoteAddrKeyResolver() {
        return exchange -> Mono.just(Objects.requireNonNull(exchange.getRequest().getRemoteAddress()).getAddress().getHostAddress());
    }
    
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // In a real implementation, this would extract the user ID from the authentication context
            // For now, we'll use a default user ID
            String userId = extractUserId(exchange);
            return Mono.just(userId);
        };
    }
    
    private String extractUserId(org.springframework.web.server.ServerWebExchange exchange) {
        // This is a simplified implementation
        // In a real application, you would extract the user ID from JWT token, session, etc.
        return "default-user";
    }
}
