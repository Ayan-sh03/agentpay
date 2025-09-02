package com.payment.payment.agent.security;

import com.payment.payment.agent.service.AgentAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reactive authentication manager that validates JWT using AgentAuthenticationService
 */
@Component
public class JwtReactiveAuthenticationManager implements org.springframework.security.authentication.ReactiveAuthenticationManager {

    @Autowired
    private AgentAuthenticationService authService;

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String token = (String) authentication.getCredentials();
        if (token == null || token.isBlank()) {
            return Mono.error(new BadCredentialsException("Missing bearer token"));
        }

        return Mono.defer(() -> authService.validateToken(token)
            .map(agentContext -> {
                Collection<GrantedAuthority> authorities = buildAuthorities(agentContext.getCapabilities());
                AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(agentContext, token, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
                return (Authentication) auth;
            })
            .map(Mono::just)
            .orElseGet(() -> Mono.error(new BadCredentialsException("Invalid bearer token")))
        );
    }

    private Collection<GrantedAuthority> buildAuthorities(Set<String> capabilities) {
        if (capabilities == null) {
            return Set.<GrantedAuthority>of();
        }
        return capabilities.stream()
            .map(cap -> new SimpleGrantedAuthority("CAP_" + cap.toUpperCase()))
            .collect(Collectors.toSet());
    }
}


