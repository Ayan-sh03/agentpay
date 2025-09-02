package com.payment.payment.agent.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

@Service
public class OpaPolicyService {
    
    @Autowired
    private WebClient opaWebClient;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @Value("${opa.url}")
    private String opaUrl;
    
    @Value("${app.opa.policy.path:classpath:policies/comprehensive_policy.rego}")
    private String policyPath;
    
    private String currentPolicy;
    private static final Logger log = LoggerFactory.getLogger(OpaPolicyService.class);
    
    @PostConstruct
    public void loadInitialPolicy() {
        try {
            loadPolicyFromPath();
            updateOpaPolicy();
        } catch (Exception e) {
            log.warn("opa:policy:init failed: {}", e.getMessage());
        }
    }
    
    public void loadPolicyFromPath() throws IOException {
        Resource resource = resourceLoader.getResource(policyPath);
        Path path = Paths.get(resource.getURI());
        currentPolicy = Files.readString(path);
    }
    
    public void updateOpaPolicy() {
        // Update OPA with the new policy
        String policyName = "payments";
        
        opaWebClient.put()
            .uri("/v1/policies/" + policyName)
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue(currentPolicy)
            .exchangeToMono(clientResponse -> {
                if (clientResponse.statusCode().is2xxSuccessful()) {
                    return clientResponse.bodyToMono(String.class)
                        .defaultIfEmpty("")
                        .doOnNext(body -> log.info("opa:policy:update success"));
                }
                return clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .doOnNext(body -> log.warn("opa:policy:update failed: status={} body={}", clientResponse.statusCode(), body))
                    .then(Mono.empty());
            })
            .subscribe();
    }
    
    // Scheduled task to periodically check for policy updates
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void checkForPolicyUpdates() {
        try {
            String previousPolicy = currentPolicy;
            loadPolicyFromPath();
            
            // If policy has changed, update OPA
            if (!currentPolicy.equals(previousPolicy)) {
                log.info("opa:policy:change detected -> updating");
                updateOpaPolicy();
            }
        } catch (Exception e) {
            log.warn("opa:policy:check failed: {}", e.getMessage());
        }
    }
    
    public String getCurrentPolicy() {
        return currentPolicy;
    }
    
    // Removed JSON wrapper; OPA expects raw Rego text for policy PUT
}