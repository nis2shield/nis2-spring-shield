package com.nis2shield.spring.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * SessionGuard provides session hijacking protection by tracking device fingerprints.
 * If the device fingerprint changes during an active session, the session is invalidated.
 *
 * <p>Fingerprint components:</p>
 * <ul>
 *   <li>User-Agent header</li>
 *   <li>Accept-Language header</li>
 *   <li>Accept-Encoding header</li>
 *   <li>Client IP address (optional)</li>
 * </ul>
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * nis2:
 *   session:
 *     guardEnabled: true
 *     includeIpInFingerprint: false  # Set to true for stricter checks
 *     invalidateOnMismatch: true
 * </pre>
 */
public class SessionGuard {

    private static final Logger log = LoggerFactory.getLogger(SessionGuard.class);
    private static final String FINGERPRINT_KEY = "NIS2_DEVICE_FINGERPRINT";
    private static final String FINGERPRINT_CREATED_AT = "NIS2_FINGERPRINT_CREATED";

    private final boolean includeIpInFingerprint;
    private final boolean invalidateOnMismatch;

    public SessionGuard(boolean includeIpInFingerprint, boolean invalidateOnMismatch) {
        this.includeIpInFingerprint = includeIpInFingerprint;
        this.invalidateOnMismatch = invalidateOnMismatch;
    }

    /**
     * Result of session validation.
     */
    public record ValidationResult(
        boolean valid,
        boolean fingerprintMismatch,
        boolean sessionInvalidated,
        String reason
    ) {
        public static ValidationResult ok() {
            return new ValidationResult(true, false, false, null);
        }
        
        public static ValidationResult newSession() {
            return new ValidationResult(true, false, false, "New fingerprint registered");
        }
        
        public static ValidationResult mismatch(boolean invalidated, String reason) {
            return new ValidationResult(false, true, invalidated, reason);
        }
    }

    /**
     * Validate the session and check for fingerprint changes.
     * 
     * @param request The HTTP request
     * @return ValidationResult indicating whether the session is valid
     */
    public ValidationResult validateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        
        if (session == null) {
            // No session exists, nothing to validate
            return ValidationResult.ok();
        }

        String currentFingerprint = generateFingerprint(request);
        String storedFingerprint = (String) session.getAttribute(FINGERPRINT_KEY);

        if (storedFingerprint == null) {
            // First request in this session, store fingerprint
            session.setAttribute(FINGERPRINT_KEY, currentFingerprint);
            session.setAttribute(FINGERPRINT_CREATED_AT, System.currentTimeMillis());
            log.debug("Registered device fingerprint for session {}", session.getId());
            return ValidationResult.newSession();
        }

        if (!storedFingerprint.equals(currentFingerprint)) {
            // Fingerprint mismatch - potential session hijacking
            log.warn("Device fingerprint mismatch detected for session {}. " +
                     "Stored: {}, Current: {}", 
                     session.getId(), 
                     storedFingerprint.substring(0, 8) + "...", 
                     currentFingerprint.substring(0, 8) + "...");

            if (invalidateOnMismatch) {
                session.invalidate();
                log.warn("Session {} invalidated due to fingerprint mismatch", session.getId());
                return ValidationResult.mismatch(true, "Session invalidated due to device fingerprint change");
            } else {
                return ValidationResult.mismatch(false, "Device fingerprint mismatch detected");
            }
        }

        return ValidationResult.ok();
    }

    /**
     * Register a fingerprint for a new session.
     * Call this after successful authentication.
     */
    public void registerFingerprint(HttpServletRequest request) {
        HttpSession session = request.getSession(true);
        String fingerprint = generateFingerprint(request);
        session.setAttribute(FINGERPRINT_KEY, fingerprint);
        session.setAttribute(FINGERPRINT_CREATED_AT, System.currentTimeMillis());
        log.debug("Registered device fingerprint for session {}", session.getId());
    }

    /**
     * Generate a device fingerprint from request headers.
     */
    public String generateFingerprint(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        
        // User-Agent
        String userAgent = request.getHeader("User-Agent");
        sb.append(userAgent != null ? userAgent : "unknown");
        sb.append("|");
        
        // Accept-Language
        String acceptLanguage = request.getHeader("Accept-Language");
        sb.append(acceptLanguage != null ? acceptLanguage : "unknown");
        sb.append("|");
        
        // Accept-Encoding
        String acceptEncoding = request.getHeader("Accept-Encoding");
        sb.append(acceptEncoding != null ? acceptEncoding : "unknown");
        
        // Optionally include IP address (stricter, but may cause issues with proxies)
        if (includeIpInFingerprint) {
            sb.append("|");
            sb.append(getClientIp(request));
        }

        return hashFingerprint(sb.toString());
    }

    /**
     * Hash the fingerprint for storage.
     */
    private String hashFingerprint(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available in Java
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Get client IP considering proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * Check if a session has a fingerprint registered.
     */
    public boolean hasFingerprint(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && session.getAttribute(FINGERPRINT_KEY) != null;
    }

    /**
     * Get the fingerprint creation time for a session.
     */
    public Long getFingerprintCreatedAt(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        return (Long) session.getAttribute(FINGERPRINT_CREATED_AT);
    }
}
