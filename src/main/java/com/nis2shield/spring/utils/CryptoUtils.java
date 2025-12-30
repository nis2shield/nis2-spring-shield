package com.nis2shield.spring.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class CryptoUtils {

    private static final Logger logger = LoggerFactory.getLogger(CryptoUtils.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private final SecretKey secretKey;

    public CryptoUtils(String base64Key) {
        if (base64Key == null || base64Key.isEmpty()) {
            this.secretKey = null;
            return;
        }
        try {
            // Assume the user provides a Base64 encoded 32-byte key for AES-256
            byte[] decodedKey = Base64.getDecoder().decode(base64Key);
            this.secretKey = new SecretKeySpec(decodedKey, "AES");
        } catch (IllegalArgumentException e) {
            logger.error("Invalid Encryption Key format (Base64 expected)", e);
            throw new RuntimeException("Invalid Encryption Key");
        }
    }

    public String encrypt(String plaintext) {
        if (secretKey == null || plaintext == null) return plaintext;

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to cipherText to allow decryption
            byte[] message = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, message, 0, iv.length);
            System.arraycopy(cipherText, 0, message, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(message);
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            return "[ENCRYPTION_ERROR]";
        }
    }
}
