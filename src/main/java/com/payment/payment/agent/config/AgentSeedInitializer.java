package com.payment.payment.agent.config;

import com.payment.payment.agent.model.AgentCredentials;
import com.payment.payment.agent.repository.AgentCredentialsRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;

@Configuration
public class AgentSeedInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentSeedInitializer.class);

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.apiKey:}")
    private String seedApiKey;

    @Value("${app.seed.agentId:agent-001}")
    private String seedAgentId;

    @Value("${app.seed.ownerId:owner-abc}")
    private String seedOwnerId;

    @Value("${app.apikey.hmacSecret:}")
    private String apiKeyHmacSecret;

    @Autowired
    private AgentCredentialsRepository agentRepository;

    @PostConstruct
    public void seed() {
        if (!seedEnabled) return;
        if (seedApiKey == null || seedApiKey.isBlank()) {
            LOGGER.warn("Seed enabled but app.seed.apiKey missing; skipping");
            return;
        }

        if (agentRepository.findById(seedAgentId).isPresent()) {
            LOGGER.info("Seed agent already exists: {}", seedAgentId);
            return;
        }

        String apiKeyHash = hmacHex(seedApiKey, apiKeyHmacSecret);

        AgentCredentials creds = new AgentCredentials();
        creds.setAgentId(seedAgentId);
        creds.setOwnerId(seedOwnerId);
        creds.setApiKeyHash(apiKeyHash);
        creds.setAgentType("bot");
        creds.setIsActive(true);
        creds.setCreatedAt(LocalDateTime.now());

        agentRepository.save(creds);
        LOGGER.info("Seeded agent {}", seedAgentId);
    }

    private static byte[] decodeIfBase64ElseUtf8(String value) {
        if (value == null) return null;
        try { return Base64.getDecoder().decode(value); } catch (IllegalArgumentException e) { return value.getBytes(StandardCharsets.UTF_8); }
    }

    private static String hmacHex(String apiKey, String secret) {
        byte[] secretBytes = decodeIfBase64ElseUtf8(secret);
        if (secretBytes == null || secretBytes.length < 32) {
            throw new IllegalStateException("Invalid app.apikey.hmacSecret; cannot seed agent");
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA256"));
            byte[] digest = mac.doFinal(apiKey.getBytes(StandardCharsets.UTF_8));
            char[] HEX = "0123456789abcdef".toCharArray();
            char[] out = new char[digest.length * 2];
            for (int i = 0, j = 0; i < digest.length; i++) {
                int v = digest[i] & 0xFF;
                out[j++] = HEX[v >>> 4];
                out[j++] = HEX[v & 0x0F];
            }
            return new String(out);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to HMAC api key for seeding", e);
        }
    }
}


