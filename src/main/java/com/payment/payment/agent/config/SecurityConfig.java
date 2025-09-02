package com.payment.payment.agent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import javax.crypto.SecretKey;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import reactor.core.publisher.Mono;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Arrays;
import java.util.Collections;

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
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(exchanges -> exchanges
                .pathMatchers(HttpMethod.POST, "/api/v1/auth/token").permitAll()
                .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            .build();
    }

    private Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter());
        return new ReactiveJwtAuthenticationConverterAdapter(jwtConverter);
    }

    private Converter<Jwt, Collection<GrantedAuthority>> grantedAuthoritiesConverter() {
        return jwt -> {
            Object caps = jwt.getClaims().get("capabilities");
            if (caps instanceof String s && !s.isBlank()) {
                return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(v -> !v.isEmpty())
                    .map(v -> new SimpleGrantedAuthority("CAP_" + v.toUpperCase()))
                    .collect(Collectors.toSet());
            }
            return Collections.emptySet();
        };
    }

    @Bean
    @Primary
    public ReactiveJwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return NimbusReactiveJwtDecoder.withSecretKey(key)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }
}