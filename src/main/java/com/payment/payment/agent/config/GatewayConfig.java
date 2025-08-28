package com.payment.payment.agent.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, KeyResolver userKeyResolver) {
        return builder.routes()
                .route("payment_route", r -> r.path("/api/v1/**")
                        .filters(f -> f.requestRateLimiter(c -> 
                            c.setKeyResolver(userKeyResolver)
                            .setRateLimiter(redisRateLimiter())))
                        .uri("http://localhost:8080"))
                .build();
    }
    
    @Bean
    public org.springframework.cloud.gateway.filter.ratelimit.RateLimiter<?> redisRateLimiter() {
        // Configuration for Redis-based rate limiter
        // This would be configured via properties in a real implementation
        return new org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter(10, 20);
    }
}
