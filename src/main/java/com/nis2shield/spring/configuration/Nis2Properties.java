package com.nis2shield.spring.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nis2")
public class Nis2Properties {

    /**
     * Enable or disable NIS2 Shield entirely.
     */
    private boolean enabled = true;

    /**
     * Configuration for forensic logging.
     */
    private Logging logging = new Logging();

    /**
     * Secret key for integrity signing (HMAC).
     */
    private String integrityKey;

    /**
     * Secret key for PII encryption (AES).
     */
    private String encryptionKey;

    private ActiveDefense activeDefense = new ActiveDefense();

    public static class Logging {
        private boolean enabled = true;
        private boolean anonymizeIp = true;
        private boolean encryptPii = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isAnonymizeIp() { return anonymizeIp; }
        public void setAnonymizeIp(boolean anonymizeIp) { this.anonymizeIp = anonymizeIp; }
        public boolean isEncryptPii() { return encryptPii; }
        public void setEncryptPii(boolean encryptPii) { this.encryptPii = encryptPii; }
    }

    public static class ActiveDefense {
        private boolean rateLimitEnabled = true;
        private long rateLimitCapacity = 100; // requests per window
        private long rateLimitWindowSeconds = 60; 
        private boolean blockTorExitNodes = false; 

        public boolean isRateLimitEnabled() { return rateLimitEnabled; }
        public void setRateLimitEnabled(boolean rateLimitEnabled) { this.rateLimitEnabled = rateLimitEnabled; }
        public long getRateLimitCapacity() { return rateLimitCapacity; }
        public void setRateLimitCapacity(long rateLimitCapacity) { this.rateLimitCapacity = rateLimitCapacity; }
        public long getRateLimitWindowSeconds() { return rateLimitWindowSeconds; }
        public void setRateLimitWindowSeconds(long rateLimitWindowSeconds) { this.rateLimitWindowSeconds = rateLimitWindowSeconds; }
        public boolean isBlockTorExitNodes() { return blockTorExitNodes; }
        public void setBlockTorExitNodes(boolean blockTorExitNodes) { this.blockTorExitNodes = blockTorExitNodes; }
    }

    /**
     * Configuration for security headers.
     */
    private SecurityHeaders securityHeaders = new SecurityHeaders();

    public static class SecurityHeaders {
        private boolean enabled = true;
        private boolean hstsEnabled = true;
        private boolean xContentTypeOptionsEnabled = true;
        private boolean xFrameOptionsEnabled = true;
        private boolean cspEnabled = true;
        private boolean referrerPolicyEnabled = true;
        private boolean permissionsPolicyEnabled = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public boolean isHstsEnabled() { return hstsEnabled; }
        public void setHstsEnabled(boolean hstsEnabled) { this.hstsEnabled = hstsEnabled; }
        public boolean isXContentTypeOptionsEnabled() { return xContentTypeOptionsEnabled; }
        public void setXContentTypeOptionsEnabled(boolean xContentTypeOptionsEnabled) { this.xContentTypeOptionsEnabled = xContentTypeOptionsEnabled; }
        public boolean isXFrameOptionsEnabled() { return xFrameOptionsEnabled; }
        public void setXFrameOptionsEnabled(boolean xFrameOptionsEnabled) { this.xFrameOptionsEnabled = xFrameOptionsEnabled; }
        public boolean isCspEnabled() { return cspEnabled; }
        public void setCspEnabled(boolean cspEnabled) { this.cspEnabled = cspEnabled; }
        public boolean isReferrerPolicyEnabled() { return referrerPolicyEnabled; }
        public void setReferrerPolicyEnabled(boolean referrerPolicyEnabled) { this.referrerPolicyEnabled = referrerPolicyEnabled; }
        public boolean isPermissionsPolicyEnabled() { return permissionsPolicyEnabled; }
        public void setPermissionsPolicyEnabled(boolean permissionsPolicyEnabled) { this.permissionsPolicyEnabled = permissionsPolicyEnabled; }
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Logging getLogging() { return logging; }
    public void setLogging(Logging logging) { this.logging = logging; }
    public ActiveDefense getActiveDefense() { return activeDefense; }
    public void setActiveDefense(ActiveDefense activeDefense) { this.activeDefense = activeDefense; }
    public String getIntegrityKey() { return integrityKey; }
    public void setIntegrityKey(String integrityKey) { this.integrityKey = integrityKey; }
    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    public SecurityHeaders getSecurityHeaders() { return securityHeaders; }
    public void setSecurityHeaders(SecurityHeaders securityHeaders) { this.securityHeaders = securityHeaders; }
}
