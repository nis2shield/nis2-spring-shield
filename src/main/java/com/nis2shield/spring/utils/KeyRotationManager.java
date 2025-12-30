package com.nis2shield.spring.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

/**
 * KeyRotationManager provides a stub implementation for managing 
 * encryption key rotation schedules.
 * 
 * <p>In enterprise environments, encryption keys should be rotated 
 * periodically to limit the impact of potential key compromise.
 * NIS2 Art. 21 emphasizes cryptographic security measures.</p>
 * 
 * <p><b>Note:</b> This is a stub implementation. In production, integrate 
 * with a proper Key Management Service (KMS) like AWS KMS, HashiCorp Vault,
 * or Azure Key Vault.</p>
 */
public class KeyRotationManager {

    private static final Logger logger = LoggerFactory.getLogger(KeyRotationManager.class);
    
    private final AtomicReference<SecretKey> currentKey = new AtomicReference<>();
    private final AtomicReference<Instant> keyCreatedAt = new AtomicReference<>();
    private final long rotationIntervalDays;
    
    /**
     * Creates a KeyRotationManager with the specified rotation interval.
     * 
     * @param rotationIntervalDays days between key rotations (default: 90)
     */
    public KeyRotationManager(long rotationIntervalDays) {
        this.rotationIntervalDays = rotationIntervalDays > 0 ? rotationIntervalDays : 90;
        logger.info("KeyRotationManager initialized with rotation interval: {} days", this.rotationIntervalDays);
    }
    
    /**
     * Creates a KeyRotationManager with default 90-day rotation interval.
     */
    public KeyRotationManager() {
        this(90);
    }
    
    /**
     * Generates a new AES-256 key and sets it as the current key.
     * 
     * @return Base64-encoded new key
     */
    public String rotateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            SecretKey newKey = keyGen.generateKey();
            
            currentKey.set(newKey);
            keyCreatedAt.set(Instant.now());
            
            String base64Key = Base64.getEncoder().encodeToString(newKey.getEncoded());
            logger.info("Key rotated successfully at {}", keyCreatedAt.get());
            
            return base64Key;
        } catch (Exception e) {
            logger.error("Failed to rotate key", e);
            throw new RuntimeException("Key rotation failed", e);
        }
    }
    
    /**
     * Checks if the current key needs rotation based on age.
     * 
     * @return true if key is older than the rotation interval
     */
    public boolean isRotationNeeded() {
        Instant created = keyCreatedAt.get();
        if (created == null) {
            return true; // No key set yet
        }
        
        Instant expirationDate = created.plus(rotationIntervalDays, ChronoUnit.DAYS);
        return Instant.now().isAfter(expirationDate);
    }
    
    /**
     * Gets the current key creation timestamp.
     * 
     * @return key creation instant, or null if no key set
     */
    public Instant getKeyCreatedAt() {
        return keyCreatedAt.get();
    }
    
    /**
     * Gets days until next rotation.
     * 
     * @return days remaining, or 0 if rotation is due
     */
    public long getDaysUntilRotation() {
        Instant created = keyCreatedAt.get();
        if (created == null) {
            return 0;
        }
        
        Instant expirationDate = created.plus(rotationIntervalDays, ChronoUnit.DAYS);
        long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), expirationDate);
        return Math.max(0, daysRemaining);
    }
}
