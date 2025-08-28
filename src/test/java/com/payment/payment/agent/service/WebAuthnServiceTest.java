package com.payment.payment.agent.service;

import com.payment.payment.agent.model.PurchaseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class WebAuthnServiceTest {

    private WebAuthnService webAuthnService;

    @BeforeEach
    void setUp() {
        webAuthnService = new WebAuthnService();
        // Set test properties
        ReflectionTestUtils.setField(webAuthnService, "relyingPartyId", "localhost");
        ReflectionTestUtils.setField(webAuthnService, "relyingPartyName", "Test Payment Agent");
        ReflectionTestUtils.setField(webAuthnService, "origin", "http://localhost:8080");
    }

    @Test
    void testIsStepUpRequired_WithLowValueTransaction_ShouldReturnFalse() {
        // Given
        PurchaseRequest lowValueRequest = new PurchaseRequest();
        lowValueRequest.setAmount(100.0);

        // When
        boolean result = webAuthnService.isStepUpRequired("txn123", lowValueRequest);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsStepUpRequired_WithHighValueTransaction_ShouldReturnTrue() {
        // Given
        PurchaseRequest highValueRequest = new PurchaseRequest();
        highValueRequest.setAmount(1500.0);

        // When
        boolean result = webAuthnService.isStepUpRequired("txn123", highValueRequest);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsStepUpRequired_WithNonPurchaseRequest_ShouldReturnFalse() {
        // When
        boolean result = webAuthnService.isStepUpRequired("txn123", "not a purchase request");

        // Then
        assertFalse(result);
    }

    @Test
    void testStartAuthentication_WithoutRegisteredAuthenticator_ShouldThrowException() {
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> webAuthnService.startAuthentication("user123"));

        assertTrue(exception.getMessage().contains("No authenticator registered"));
    }

    @Test
    void testStartRegistration_ShouldReturnValidRequest() {
        // When
        WebAuthnService.WebAuthnRegistrationRequest request =
            webAuthnService.startRegistration("user123");

        // Then
        assertNotNull(request);
        assertNotNull(request.getChallenge());
        assertEquals("localhost", request.getRelyingPartyId());
        assertEquals("Test Payment Agent", request.getRelyingPartyName());
        assertFalse(request.getChallenge().isEmpty());
    }

    @Test
    void testHasAuthenticator_WithoutRegistration_ShouldReturnFalse() {
        // When
        boolean result = webAuthnService.hasAuthenticator("user123");

        // Then
        assertFalse(result);
    }

    @Test
    void testPerformStepUpAuthentication_WithoutAuthenticator_ShouldReturnFalse() {
        // When
        boolean result = webAuthnService.performStepUpAuthentication("user123");

        // Then
        assertFalse(result);
    }

    @Test
    void testPerformStepUpAuthentication_WithAuthenticator_ShouldReturnTrue() {
        // Given - simulate registered authenticator
        ReflectionTestUtils.setField(webAuthnService, "authenticatorStorage",
            java.util.Map.of("user123", new Object())); // Mock authenticator

        // When
        boolean result = webAuthnService.performStepUpAuthentication("user123");

        // Then
        assertTrue(result);
    }

    @Test
    void testWebAuthnAuthenticationRequest_Creation() {
        // Given
        String challenge = "test-challenge";
        String rpId = "test-rp";

        // When
        WebAuthnService.WebAuthnAuthenticationRequest request =
            new WebAuthnService.WebAuthnAuthenticationRequest(challenge, rpId);

        // Then
        assertEquals(challenge, request.getChallenge());
        assertEquals(rpId, request.getRelyingPartyId());
    }

    @Test
    void testWebAuthnRegistrationRequest_Creation() {
        // Given
        String challenge = "test-challenge";
        String rpId = "test-rp";
        String rpName = "Test RP";

        // When
        WebAuthnService.WebAuthnRegistrationRequest request =
            new WebAuthnService.WebAuthnRegistrationRequest(challenge, rpId, rpName);

        // Then
        assertEquals(challenge, request.getChallenge());
        assertEquals(rpId, request.getRelyingPartyId());
        assertEquals(rpName, request.getRelyingPartyName());
    }
}