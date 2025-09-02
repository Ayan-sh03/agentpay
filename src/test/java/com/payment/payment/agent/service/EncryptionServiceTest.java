package com.payment.payment.agent.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic tests for AES-GCM encryption
 * Ensures encryption/decryption works and is secure
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
        // Set a test key - must be exactly 32 characters for AES-256
        ReflectionTestUtils.setField(encryptionService, "encryptionKey", "test-key-12345678901234567890123");
    }

    @Test
    void testEncryptDecrypt_ValidInput_ReturnsOriginalValue() throws Exception {
        // Given
        String originalValue = "secret-payment-credentials";

        // When
        String encrypted = encryptionService.encrypt(originalValue);
        String decrypted = encryptionService.decrypt(encrypted);

        // Then
        assertEquals(originalValue, decrypted);
        assertNotEquals(originalValue, encrypted);
        assertTrue(encrypted.length() > originalValue.length()); // Should be longer due to IV + tag
    }

    @Test
    void testEncrypt_SameInput_DifferentOutputs() throws Exception {
        // Given
        String value = "test-value";

        // When - encrypt same value twice
        String encrypted1 = encryptionService.encrypt(value);
        String encrypted2 = encryptionService.encrypt(value);

        // Then - should be different due to random IV
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to same value
        assertEquals(value, encryptionService.decrypt(encrypted1));
        assertEquals(value, encryptionService.decrypt(encrypted2));
    }

    @Test 
    void testDecrypt_InvalidInput_ThrowsException() {
        // Given
        String invalidEncryptedValue = "not-valid-encrypted-data";

        // When/Then
        assertThrows(Exception.class, () -> {
            encryptionService.decrypt(invalidEncryptedValue);
        });
    }
}