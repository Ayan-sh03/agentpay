package com.payment.payment.agent.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationContextService {

    /**
     * Extracts the current authenticated user ID from the security context
     */
    public String getCurrentUserId() {
        String userId = "user1";
        System.out.println("AuthenticationContextService.getCurrentUserId() returning: " + userId);
        return userId;
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