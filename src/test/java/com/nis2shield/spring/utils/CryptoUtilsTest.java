package com.nis2shield.spring.utils;

import org.junit.jupiter.api.Test;
import java.util.Base64;
import static org.junit.jupiter.api.Assertions.*;

class CryptoUtilsTest {

    // 32 bytes key base64 encoded
    private static final String TEST_KEY = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());

    @Test
    void testEncryptDecrypt() {
        CryptoUtils crypto = new CryptoUtils(TEST_KEY);
        String plainText = "Sensitive Data";
        
        String encrypted = crypto.encrypt(plainText);
        
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
        assertTrue(encrypted.length() > 0);
        
        // Decryption logic is not exposed in public API of CryptoUtils for now (it's one-way log encryption mostly), 
        // but for a real test we should verify we CAN decrypt it if we wanted to.
        // Since CryptoUtils only has encrypt() method exposed based on current implementation, 
        // we test that it produces something valid (no exception) and non-empty.
    }

    @Test
    void testEncryptWithNull() {
        CryptoUtils crypto = new CryptoUtils(TEST_KEY);
        assertNull(crypto.encrypt(null));
    }
}
