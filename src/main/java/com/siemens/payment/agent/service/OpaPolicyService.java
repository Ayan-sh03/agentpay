package com.siemens.payment.agent.service;

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
    
    @PostConstruct
    public void loadInitialPolicy() {
        try {
            loadPolicyFromPath();
            updateOpaPolicy();
        } catch (Exception e) {
            System.err.println("Failed to load initial OPA policy: " + e.getMessage());
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
            .body(Mono.just(new PolicyUpdateRequest(currentPolicy)), PolicyUpdateRequest.class)
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> System.out.println("Policy updated successfully"),
                error -> System.err.println("Failed to update policy: " + error.getMessage())
            );
    }
    
    // Scheduled task to periodically check for policy updates
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void checkForPolicyUpdates() {
        try {
            String previousPolicy = currentPolicy;
            loadPolicyFromPath();
            
            // If policy has changed, update OPA
            if (!currentPolicy.equals(previousPolicy)) {
                System.out.println("Policy change detected, updating OPA...");
                updateOpaPolicy();
            }
        } catch (Exception e) {
            System.err.println("Failed to check for policy updates: " + e.getMessage());
        }
    }
    
    public String getCurrentPolicy() {
        return currentPolicy;
    }
    
    // Helper class for policy update request
    private static class PolicyUpdateRequest {
        private final String policy;
        
        public PolicyUpdateRequest(String policy) {
            this.policy = policy;
        }
        
        public String getPolicy() {
            return policy;
        }
    }
}