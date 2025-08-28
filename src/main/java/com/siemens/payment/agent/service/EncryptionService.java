package com.siemens.payment.agent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Service
public class EncryptionService {

    private static final String ALGORITHM = "AES";

    @Value("${app.encryption.key}")
    private String encryptionKey;

    public String encrypt(String value) throws Exception {
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encryptedByteValue = cipher.doFinal(value.getBytes("utf-8"));
        return Base64.getEncoder().encodeToString(encryptedByteValue);
    }

    public String decrypt(String value) throws Exception {
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decryptedByteValue = cipher.doFinal(Base64.getDecoder().decode(value));
        return new String(decryptedByteValue, "utf-8");
    }
}
