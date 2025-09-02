package com.payment.payment.agent.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Extracts Bearer token from Authorization header for WebFlux
 */
@Component
public class BearerTokenServerAuthenticationConverter implements org.springframework.security.web.server.authentication.ServerAuthenticationConverter {
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth == null || !auth.startsWith("Bearer ")) {
            return Mono.empty();
        }
        String token = auth.substring(7);
        return Mono.just(new UsernamePasswordAuthenticationToken(null, token));
    }
}


