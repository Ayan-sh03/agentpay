package com.payment.payment.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebAuthnService {

    // In-memory storage for demo purposes - in production, use persistent storage
    private final Map<String, String> authenticatorStorage = new ConcurrentHashMap<>();
    private final Map<String, String> challengeStorage = new ConcurrentHashMap<>();

    @Value("${app.webauthn.rp.id:localhost}")
    private String relyingPartyId;

    @Value("${app.webauthn.rp.name:Payment Agent}")
    private String relyingPartyName;

    @Value("${app.webauthn.origin:http://localhost:8080}")
    private String origin;

    /**
     * Determines if step-up authentication is required based on transaction risk
     */
    public boolean isStepUpRequired(String transactionId, Object riskContext) {
        if (!(riskContext instanceof com.payment.payment.agent.model.PurchaseRequest)) {
            return false;
        }

        com.payment.payment.agent.model.PurchaseRequest request =
            (com.payment.payment.agent.model.PurchaseRequest) riskContext;

        // Step-up authentication required for high-value transactions (> $1000)
        return request.getAmount() > 1000.0;
    }

    /**
     * Initiates WebAuthn authentication ceremony
     */
    public WebAuthnAuthenticationRequest startAuthentication(String userId) {
        if (!authenticatorStorage.containsKey(userId)) {
            throw new IllegalStateException("No authenticator registered for user: " + userId);
        }

        String challenge = Base64.getEncoder().encodeToString(java.util.UUID.randomUUID().toString().getBytes());
        challengeStorage.put(userId, challenge);

        return new WebAuthnAuthenticationRequest(challenge, relyingPartyId);
    }

    /**
     * Verifies WebAuthn authentication response
     */
    public boolean verifyAuthentication(String userId, String credentialId, String authenticatorData,
                                      String clientDataJSON, String signature) {
        try {
            String storedAuthenticator = authenticatorStorage.get(userId);
            if (storedAuthenticator == null) {
                return false;
            }

            String challenge = challengeStorage.get(userId);
            if (challenge == null) {
                return false;
            }

            // For demo purposes, we'll implement a simplified verification
            // In production, you would use the full WebAuthnManager.validateAuthentication
            if (authenticatorData != null && clientDataJSON != null && signature != null) {
                challengeStorage.remove(userId);
                return true;
            }

            return false;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Registers a new WebAuthn authenticator for a user
     */
    public boolean registerAuthenticator(String userId, String attestationObject, String clientDataJSON) {
        try {
            if (attestationObject == null || clientDataJSON == null) {
                return false;
            }

            String challenge = challengeStorage.get(userId);
            if (challenge == null) {
                return false;
            }

            // For demo purposes, store a simple authenticator identifier
            // In production, you would validate the attestation properly using WebAuthnManager
            String authenticatorId = Base64.getEncoder().encodeToString(java.util.UUID.randomUUID().toString().getBytes());
            authenticatorStorage.put(userId, authenticatorId);
            challengeStorage.remove(userId);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Initiates WebAuthn registration ceremony
     */
    public WebAuthnRegistrationRequest startRegistration(String userId) {
        String challenge = Base64.getEncoder().encodeToString(java.util.UUID.randomUUID().toString().getBytes());
        challengeStorage.put(userId, challenge);

        return new WebAuthnRegistrationRequest(challenge, relyingPartyId, relyingPartyName);
    }

    /**
     * Performs step-up authentication for high-risk transactions
     */
    public boolean performStepUpAuthentication(String userId) {
        try {
            // Check if user has registered authenticator
            if (!authenticatorStorage.containsKey(userId)) {
                return false;
            }

            // In a real implementation, this would initiate the WebAuthn flow
            // For now, we'll simulate successful authentication
            // In production, you would:
            // 1. Generate challenge
            // 2. Send challenge to client
            // 3. Receive and verify authentication response
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if user has registered WebAuthn authenticator
     */
    public boolean hasAuthenticator(String userId) {
        return authenticatorStorage.containsKey(userId);
    }

    /**
     * Data class for WebAuthn authentication request
     */
    public static class WebAuthnAuthenticationRequest {
        private final String challenge;
        private final String relyingPartyId;

        public WebAuthnAuthenticationRequest(String challenge, String relyingPartyId) {
            this.challenge = challenge;
            this.relyingPartyId = relyingPartyId;
        }

        public String getChallenge() { return challenge; }
        public String getRelyingPartyId() { return relyingPartyId; }
    }

    /**
     * Data class for WebAuthn registration request
     */
    public static class WebAuthnRegistrationRequest {
        private final String challenge;
        private final String relyingPartyId;
        private final String relyingPartyName;

        public WebAuthnRegistrationRequest(String challenge, String relyingPartyId, String relyingPartyName) {
            this.challenge = challenge;
            this.relyingPartyId = relyingPartyId;
            this.relyingPartyName = relyingPartyName;
        }

        public String getChallenge() { return challenge; }
        public String getRelyingPartyId() { return relyingPartyId; }
        public String getRelyingPartyName() { return relyingPartyName; }
    }
}