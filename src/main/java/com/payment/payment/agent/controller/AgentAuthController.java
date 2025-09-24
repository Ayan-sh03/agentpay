package com.payment.payment.agent.controller;

import com.payment.payment.agent.service.AgentAuthenticationService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Authentication endpoints for AI agents
 * Converts API keys to JWT tokens for subsequent requests
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AgentAuthController {

    @Autowired
    private AgentAuthenticationService agentAuthService;

    /**
     * Agent authentication endpoint
     * Exchanges API key for JWT token
     * 
     * Usage: POST /api/v1/auth/token with {"apiKey": "your-agent-api-key"}
     */
    @PostMapping("/token")
    public ResponseEntity<AuthResponse> authenticateAgent(@Valid @RequestBody AuthRequest request) {
        return agentAuthService.authenticateAgent(request.getApiKey())
            .map(jwt -> ResponseEntity.ok(new AuthResponse(jwt, "Bearer", agentAuthService.getJwtExpirationSeconds())))
            .orElse(ResponseEntity.status(401)
                .body(new AuthResponse(null, null, 0, "Invalid API key")));
    }

    /**
     * Validate current JWT token
     * Useful for agents to check if their token is still valid
     */
    @GetMapping("/validate")
    public ResponseEntity<String> validateToken() {
        // If we reach here, Spring Security already validated the JWT
        return ResponseEntity.ok("Token is valid");
    }

    /**
     * Request body for agent authentication
     */
    @Data
    public static class AuthRequest {
        @NotBlank(message = "API key is required")
        private String apiKey;
    }

    /**
     * Response for successful authentication
     */
    @Data
    public static class AuthResponse {
        private String accessToken;
        private String tokenType;
        private int expiresIn;
        private String error;

        public AuthResponse(String accessToken, String tokenType, int expiresIn) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
        }

        public AuthResponse(String accessToken, String tokenType, int expiresIn, String error) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.error = error;
        }
    }
}