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

    /**
     * SIEM integration configuration.
     */
    private Siem siem = new Siem();

    public static class Siem {
        private Splunk splunk = new Splunk();
        private Datadog datadog = new Datadog();
        private QRadar qradar = new QRadar();
        private Graylog graylog = new Graylog();

        public static class Splunk {
            private boolean enabled = false;
            private String url;
            private String token;
            private String index = "main";
            private String source = "nis2-spring-shield";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public String getToken() { return token; }
            public void setToken(String token) { this.token = token; }
            public String getIndex() { return index; }
            public void setIndex(String index) { this.index = index; }
            public String getSource() { return source; }
            public void setSource(String source) { this.source = source; }
        }

        public static class Datadog {
            private boolean enabled = false;
            private String apiKey;
            private String site = "datadoghq.com";
            private String service = "nis2-spring-shield";
            private String source = "spring";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getApiKey() { return apiKey; }
            public void setApiKey(String apiKey) { this.apiKey = apiKey; }
            public String getSite() { return site; }
            public void setSite(String site) { this.site = site; }
            public String getService() { return service; }
            public void setService(String service) { this.service = service; }
            public String getSource() { return source; }
            public void setSource(String source) { this.source = source; }
        }

        public static class QRadar {
            private boolean enabled = false;
            private String host;
            private int port = 514;
            private String deviceVendor = "NIS2Shield";
            private String deviceProduct = "SpringShield";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getHost() { return host; }
            public void setHost(String host) { this.host = host; }
            public int getPort() { return port; }
            public void setPort(int port) { this.port = port; }
            public String getDeviceVendor() { return deviceVendor; }
            public void setDeviceVendor(String deviceVendor) { this.deviceVendor = deviceVendor; }
            public String getDeviceProduct() { return deviceProduct; }
            public void setDeviceProduct(String deviceProduct) { this.deviceProduct = deviceProduct; }
        }

        public static class Graylog {
            private boolean enabled = false;
            private String url;
            private String facility = "nis2-spring-shield";

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public String getFacility() { return facility; }
            public void setFacility(String facility) { this.facility = facility; }
        }

        public Splunk getSplunk() { return splunk; }
        public void setSplunk(Splunk splunk) { this.splunk = splunk; }
        public Datadog getDatadog() { return datadog; }
        public void setDatadog(Datadog datadog) { this.datadog = datadog; }
        public QRadar getQradar() { return qradar; }
        public void setQradar(QRadar qradar) { this.qradar = qradar; }
        public Graylog getGraylog() { return graylog; }
        public void setGraylog(Graylog graylog) { this.graylog = graylog; }
    }

    /**
     * Webhook notifications configuration.
     */
    private Notifications notifications = new Notifications();

    public static class Notifications {
        private Webhook webhook = new Webhook();

        public static class Webhook {
            private boolean enabled = false;
            private String url;
            private java.util.List<String> events = java.util.List.of(
                "RATE_LIMIT_EXCEEDED", "IP_BLOCKED", "TOR_BLOCKED", "HIGH_RISK_REQUEST"
            );
            private int retryAttempts = 3;

            public boolean isEnabled() { return enabled; }
            public void setEnabled(boolean enabled) { this.enabled = enabled; }
            public String getUrl() { return url; }
            public void setUrl(String url) { this.url = url; }
            public java.util.List<String> getEvents() { return events; }
            public void setEvents(java.util.List<String> events) { this.events = events; }
            public int getRetryAttempts() { return retryAttempts; }
            public void setRetryAttempts(int retryAttempts) { this.retryAttempts = retryAttempts; }
        }

        public Webhook getWebhook() { return webhook; }
        public void setWebhook(Webhook webhook) { this.webhook = webhook; }
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
    public Siem getSiem() { return siem; }
    public void setSiem(Siem siem) { this.siem = siem; }
    public Notifications getNotifications() { return notifications; }
    public void setNotifications(Notifications notifications) { this.notifications = notifications; }
}
