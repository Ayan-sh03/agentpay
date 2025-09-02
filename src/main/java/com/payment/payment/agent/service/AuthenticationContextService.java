package com.payment.payment.agent.service;

import com.payment.payment.agent.model.AgentContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthenticationContextService {

    @Autowired
    private AgentAuthenticationService agentAuthService;

    /**
     * Get current authenticated agent context
     * This is the main method other services should use
     */
    public Mono<AgentContext> getCurrentAgentContext() {
        return ReactiveSecurityContextHolder.getContext()
            .map(securityContext -> securityContext.getAuthentication())
            .filter(auth -> auth != null && auth.getPrincipal() instanceof Jwt)
            .map(auth -> extractAgentContextFromJwt((Jwt) auth.getPrincipal()))
            .switchIfEmpty(Mono.error(new SecurityException("Unauthorized: missing or invalid JWT")));
    }
    
    /**
     * Get current agent ID (backward compatibility)
     */
    public Mono<String> getCurrentUserId() {
        return getCurrentAgentContext().map(AgentContext::getAgentId);
    }
    
    /**
     * Get current agent owner ID  
     */
    public Mono<String> getCurrentOwnerId() {
        return getCurrentAgentContext().map(AgentContext::getOwnerId);
    }

    /**
     * Demo agent context for development
     * TODO: Remove this in production
     */
    // Removed demo fallback

    private AgentContext extractAgentContextFromJwt(Jwt jwt) {
        java.util.Map<String, Object> claims = jwt.getClaims();
        String capabilitiesStr = asString(claims.get("capabilities"));
        java.util.HashSet<String> capabilities = new java.util.HashSet<>();
        if (capabilitiesStr != null && !capabilitiesStr.trim().isEmpty()) {
            for (String s : capabilitiesStr.split(",")) {
                String v = s.trim();
                if (!v.isEmpty()) capabilities.add(v);
            }
        }

        return AgentContext.builder()
            .agentId(asString(claims.get("agent_id")))
            .ownerId(asString(claims.get("owner_id")))
            .agentType(asString(claims.get("agent_type")))
            .capabilities(capabilities)
            .dailySpendLimit(asDouble(claims.get("daily_spend_limit")))
            .monthlySpendLimit(asDouble(claims.get("monthly_spend_limit")))
            .perTransactionLimit(asDouble(claims.get("per_transaction_limit")))
            .accessLevel(asString(claims.get("access_level")))
            .isActive(true)
            .build();
    }

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private Double asDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception e) { return null; }
    }

    /**
     * Extracts user ID from JWT token
     */
    private String extractUserIdFromJwt(Jwt jwt) {
        // Try common JWT claims for user ID
        String userId = jwt.getClaimAsString("sub");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        userId = jwt.getClaimAsString("user_id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        userId = jwt.getClaimAsString("username");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // Fallback to subject claim
        return jwt.getSubject();
    }

    /**
     * Checks if the current user has a specific role
     */
    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    /**
     * Checks if the current user has permission to override transactions
     */
    public boolean canOverrideTransactions() {
        return hasRole("ADMIN") || hasRole("SUPERVISOR") || hasRole("MANAGER");
    }

    /**
     * Gets the current user's authentication details
     */
    public Authentication getCurrentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Validates that the current user matches the requested user ID
     */
    public Mono<Void> validateUserAccess(String requestedUserId) {
        return getCurrentUserId()
            .flatMap(currentUserId -> {
                if (!currentUserId.equals(requestedUserId) && !hasRole("ADMIN")) {
                    return Mono.error(new SecurityException("User does not have permission to access this resource"));
                }
                return Mono.empty();
            });
    }
}