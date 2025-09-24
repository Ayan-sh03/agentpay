package com.payment.payment.agent.service;

import com.payment.payment.agent.model.AgentContext;
import com.payment.payment.agent.model.AgentCredentials;
import com.payment.payment.agent.repository.AgentCredentialsRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Base64;
import java.util.UUID;

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

    @Value("${app.jwt.issuer:}")
    private String jwtIssuer;

    @Value("${app.jwt.audience:}")
    private String jwtAudience;

    @Value("${app.jwt.expiration:3600}")  // 1 hour default
    private int jwtExpirationSeconds;

    @Value("${app.auth.clockSkewSeconds:60}")
    private long clockSkewSeconds;

    @Value("${app.apikey.hmacSecret:}")
    private String apiKeyHmacSecret;

    @Value("${app.apikey.allowLegacyHash:false}")
    private boolean allowLegacyApiKeyHash;

    private static final int MIN_SECRET_BYTES = 32; // 256-bit

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentAuthenticationService.class);

    private SecretKey jwtSigningKey;
    private Mac apiKeyMac;

    @PostConstruct
    void initialize() {
        // Initialize JWT signing key
        byte[] jwtSecretBytes = decodeIfBase64ElseUtf8(jwtSecret);
        if (jwtSecretBytes == null || jwtSecretBytes.length < MIN_SECRET_BYTES ||
            "default-secret-key-change-in-production".equals(jwtSecret)) {
            throw new IllegalStateException("Invalid app.jwt.secret configured; must be base64 or utf-8 with >= 32 bytes and not default");
        }
        this.jwtSigningKey = Keys.hmacShaKeyFor(jwtSecretBytes);

        if (isBlank(jwtIssuer) || isBlank(jwtAudience)) {
            throw new IllegalStateException("app.jwt.issuer and app.jwt.audience must be configured");
        }

        // Initialize API key HMAC
        byte[] apiKeySecretBytes = decodeIfBase64ElseUtf8(apiKeyHmacSecret);
        if (apiKeySecretBytes == null || apiKeySecretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("Invalid app.apikey.hmacSecret configured; must be base64 or utf-8 with >= 32 bytes");
        }
        try {
            this.apiKeyMac = Mac.getInstance("HmacSHA256");
            this.apiKeyMac.init(new SecretKeySpec(apiKeySecretBytes, "HmacSHA256"));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize API key HMAC", e);
        }
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }

    private byte[] decodeIfBase64ElseUtf8(String value) {
        if (value == null) return null;
        try {
            // Accept base64 (std) without padding issues
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            // Not base64: fall back to utf-8 bytes
            return value.getBytes(StandardCharsets.UTF_8);
        }
    }

    private SecretKey getSigningKey() {
        return this.jwtSigningKey;
    }

    /**
     * Authenticate agent using API key and return JWT token
     * This is the main entry point for agent authentication
     */
    public Optional<String> authenticateAgent(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return Optional.empty();
        }

        String trimmedKey = apiKey.trim();

        // Secure deterministic API key hash using HMAC-SHA256 (peppered)
        String apiKeyHash = hashApiKey(trimmedKey);

        Optional<AgentCredentials> agentOpt = agentRepository
            .findByApiKeyHashAndIsActive(apiKeyHash, true);

        // Optional backward compatibility with legacy demo hash
        if (agentOpt.isEmpty() && allowLegacyApiKeyHash) {
            String legacyHash = legacyHashApiKey(trimmedKey);
            agentOpt = agentRepository.findByApiKeyHashAndIsActive(legacyHash, true);
        }

        if (agentOpt.isEmpty()) {
            LOGGER.warn("Agent authentication failed: API key not recognized");
            return Optional.empty();
        }

        AgentCredentials agent = agentOpt.get();
        
        // Update last used timestamp
        agent.setLastUsedAt(LocalDateTime.now());
        agentRepository.save(agent);

        // Create JWT with agent claims
        String jwt = createAgentJwt(agent);
        LOGGER.info("Issued JWT for agentId={}", agent.getAgentId());
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
            .setId(UUID.randomUUID().toString())
            .setSubject(agent.getAgentId())  // Agent ID as subject
            .setIssuer(jwtIssuer)
            .setAudience(jwtAudience)
            .setNotBefore(now)
            .claim("agent_id", agent.getAgentId())
            .claim("owner_id", agent.getOwnerId())
            .claim("agent_type", agent.getAgentType())
            .claim("capabilities", agent.getCapabilities())
            .claim("daily_spend_limit", agent.getDailySpendLimit())
            .claim("monthly_spend_limit", agent.getMonthlySpendLimit())
            .claim("per_transaction_limit", agent.getPerTransactionLimit())
            .claim("access_level", "production")
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
        try {
            Mac mac = (Mac) apiKeyMac.clone();
            byte[] digest = mac.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (CloneNotSupportedException e) {
            // Fallback to synchronized use if clone not supported
            synchronized (this) {
                byte[] digest = apiKeyMac.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
                return toHex(digest);
            }
        }
    }

    private String legacyHashApiKey(String apiKey) {
        return String.valueOf(apiKey.hashCode());
    }

    private String toHex(byte[] bytes) {
        char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[j++] = HEX[v >>> 4];
            out[j++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    /**
     * Validate JWT token and return agent context
     * Used by Spring Security filter
     */
    public Optional<AgentContext> validateToken(String jwt) {
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .requireIssuer(jwtIssuer)
                .requireAudience(jwtAudience)
                .build()
                .parseClaimsJws(jwt)
                .getBody();

            return Optional.of(extractAgentContext(claims));
        } catch (Exception e) {
            LOGGER.warn("JWT validation failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public int getJwtExpirationSeconds() {
        return jwtExpirationSeconds;
    }
}