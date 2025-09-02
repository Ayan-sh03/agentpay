package com.payment.payment.agent.service;

import com.payment.payment.agent.model.AgentContext;
import com.payment.payment.agent.model.AgentCredentials;
import com.payment.payment.agent.repository.AgentCredentialsRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;

/**
 * Handles agent authentication using API keys and JWT tokens
 * Uses battle-tested Spring Security + JWT but adapted for AI agents
 */
@Service
public class AgentAuthenticationService {

    @Autowired
    private AgentCredentialsRepository agentRepository;

    @Value("${app.jwt.secret:default-secret-key-change-in-production}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:3600}")  // 1 hour default
    private int jwtExpirationSeconds;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * Authenticate agent using API key and return JWT token
     * This is the main entry point for agent authentication
     */
    public Optional<String> authenticateAgent(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }

        // Hash the API key for lookup (in production, use proper password hashing)
        String apiKeyHash = hashApiKey(apiKey);
        
        Optional<AgentCredentials> agentOpt = agentRepository
            .findByApiKeyHashAndIsActive(apiKeyHash, true);
        
        if (agentOpt.isEmpty()) {
            return Optional.empty();
        }

        AgentCredentials agent = agentOpt.get();
        
        // Update last used timestamp
        agent.setLastUsedAt(LocalDateTime.now());
        agentRepository.save(agent);

        // Create JWT with agent claims
        String jwt = createAgentJwt(agent);
        return Optional.of(jwt);
    }

    /**
     * Create JWT token with agent-specific claims
     * Uses Spring Security compatible format but with agent data
     */
    private String createAgentJwt(AgentCredentials agent) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + (jwtExpirationSeconds * 1000L));

        return Jwts.builder()
            .setSubject(agent.getAgentId())  // Agent ID as subject
            .claim("agent_id", agent.getAgentId())
            .claim("owner_id", agent.getOwnerId())
            .claim("agent_type", agent.getAgentType())
            .claim("capabilities", agent.getCapabilities())
            .claim("daily_spend_limit", agent.getDailySpendLimit())
            .claim("monthly_spend_limit", agent.getMonthlySpendLimit())
            .claim("per_transaction_limit", agent.getPerTransactionLimit())
            .claim("access_level", "production")  // Default for now
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Extract agent context from JWT token
     * Called by AuthenticationContextService
     */
    public AgentContext extractAgentContext(Claims claims) {
        String capabilitiesStr = claims.get("capabilities", String.class);
        HashSet<String> capabilities = new HashSet<>();
        if (capabilitiesStr != null && !capabilitiesStr.trim().isEmpty()) {
            capabilities.addAll(Arrays.asList(capabilitiesStr.split(",")));
        }

        return AgentContext.builder()
            .agentId(claims.get("agent_id", String.class))
            .ownerId(claims.get("owner_id", String.class))
            .agentType(claims.get("agent_type", String.class))
            .capabilities(capabilities)
            .dailySpendLimit(claims.get("daily_spend_limit", Double.class))
            .monthlySpendLimit(claims.get("monthly_spend_limit", Double.class))
            .perTransactionLimit(claims.get("per_transaction_limit", Double.class))
            .accessLevel(claims.get("access_level", String.class))
            .isActive(true)
            .build();
    }

    /**
     * Simple API key hashing (use bcrypt in production)
     */
    private String hashApiKey(String apiKey) {
        // For demo purposes, use simple hash
        // In production, use BCryptPasswordEncoder
        return String.valueOf(apiKey.hashCode());
    }

    /**
     * Validate JWT token and return agent context
     * Used by Spring Security filter
     */
    public Optional<AgentContext> validateToken(String jwt) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(jwt)
                .getBody();

            return Optional.of(extractAgentContext(claims));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}