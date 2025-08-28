package com.siemens.payment.agent.config;

import com.webauthn4j.springframework.security.config.configurer.WebAuthnLoginConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebAuthnConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.apply(WebAuthnLoginConfigurer.webAuthnLogin());
        return http.build();
    }
}
