package com.payment.payment.agent.service;

import com.payment.payment.agent.model.AgentContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationContextService {

    @Autowired
    private AgentAuthenticationService agentAuthService;

    /**
     * Get current authenticated agent context
     * This is the main method other services should use
     */
    public AgentContext getCurrentAgentContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return agentAuthService.extractAgentContext((io.jsonwebtoken.Claims) jwt.getClaims());
        }
        
        // For development/testing - remove in production
        return createDemoAgentContext();
    }
    
    /**
     * Get current agent ID (backward compatibility)
     */
    public String getCurrentUserId() {
        return getCurrentAgentContext().getAgentId();
    }
    
    /**
     * Get current agent owner ID  
     */
    public String getCurrentOwnerId() {
        return getCurrentAgentContext().getOwnerId();
    }

    /**
     * Demo agent context for development
     * TODO: Remove this in production
     */
    private AgentContext createDemoAgentContext() {
        return AgentContext.builder()
            .agentId("demo-agent-001")
            .agentName("Demo Shopping Agent")
            .agentType("demo-bot")
            .ownerId("dev-123")
            .ownerEmail("developer@example.com")
            .capabilities(java.util.Set.of("digital_goods", "api_calls"))
            .dailySpendLimit(1000.0)
            .monthlySpendLimit(5000.0)
            .perTransactionLimit(500.0)
            .accessLevel("sandbox")
            .isActive(true)
            .build();
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
    public void validateUserAccess(String requestedUserId) {
        String currentUserId = getCurrentUserId();

        if (!currentUserId.equals(requestedUserId) && !hasRole("ADMIN")) {
            throw new SecurityException("User does not have permission to access this resource");
        }
    }
}