package com.payment.payment.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Secure encryption service using AES-GCM
 * Fixed from the original insecure AES-ECB implementation
 */
@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits

    @Value("${app.encryption.key:change-me-in-production-32-chars}")
    private String encryptionKey;

    /**
     * Encrypt using AES-GCM with random IV
     * Format: IV + encrypted_data (both base64 encoded together)
     */
    public String encrypt(String value) throws Exception {
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
        
        byte[] encryptedData = cipher.doFinal(value.getBytes("utf-8"));
        
        // Combine IV + encrypted data
        byte[] encryptedWithIv = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(encryptedData, 0, encryptedWithIv, iv.length, encryptedData.length);
        
        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }

    /**
     * Decrypt AES-GCM encrypted data
     */
    public String decrypt(String encryptedValue) throws Exception {
        byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedValue);
        
        // Extract IV and encrypted data
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encryptedData = new byte[encryptedWithIv.length - GCM_IV_LENGTH];
        
        System.arraycopy(encryptedWithIv, 0, iv, 0, iv.length);
        System.arraycopy(encryptedWithIv, iv.length, encryptedData, 0, encryptedData.length);
        
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
        
        byte[] decryptedData = cipher.doFinal(encryptedData);
        return new String(decryptedData, "utf-8");
    }
}
