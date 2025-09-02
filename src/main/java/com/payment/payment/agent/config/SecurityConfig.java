package com.payment.payment.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for AI agents
 * Configures API-first security suitable for programmatic access
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            // CSRF not needed for API-only endpoints (agents use direct HTTP, not browsers)
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            
            // Configure for API access patterns (no HTML forms)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            
            // For demo: Allow all API endpoints (no authentication required)
            .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
            
            .build();
    }
}