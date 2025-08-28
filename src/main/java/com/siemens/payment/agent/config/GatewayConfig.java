package com.siemens.payment.agent.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder, KeyResolver keyResolver) {
        return builder.routes()
                .route("payment_route", r -> r.path("/api/v1/**")
                        .filters(f -> f.requestRateLimiter(c -> c.setKeyResolver(keyResolver)))
                        .uri("http://localhost:8080"))
                .build();
    }
}
