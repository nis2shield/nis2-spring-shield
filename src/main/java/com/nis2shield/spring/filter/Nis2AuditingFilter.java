package com.nis2shield.spring.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nis2shield.spring.configuration.Nis2Properties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
// cleaned import
import java.util.Map;

/**
 * NIS2 compliance auditing filter for Spring Boot applications.
 * Intercepts all HTTP requests, logs them in JSON format with HMAC-SHA256
 * integrity
 * signatures, and optionally encrypts PII fields.
 *
 * <p>
 * The filter is auto-configured when nis2-spring-shield is on the classpath.
 * </p>
 *
 * <p>
 * Example configuration in application.yml:
 * </p>
 * 
 * <pre>
 * nis2:
 *   enabled: true
 *   encryption-key: "VGhpcyBJcyBBIFRlc3QgS2V5IEZvciBBRVMgMjU2IQ=="
 *   integrity-key: "your-secret-hmac-key"
 *   logging:
 *     enabled: true
 *     encrypt-pii: true
 *     anonymize-ip: true
 * </pre>
 *
 * @see com.nis2shield.spring.configuration.Nis2Properties
 */
public class Nis2AuditingFilter extends OncePerRequestFilter {

    private static final Logger auditLogger = LoggerFactory.getLogger("NIS2_AUDIT_LOG");
    private final Nis2Properties properties;
    private final ObjectMapper objectMapper;
    private final com.nis2shield.spring.utils.CryptoUtils cryptoUtils;

    public Nis2AuditingFilter(Nis2Properties properties, ObjectMapper objectMapper,
            com.nis2shield.spring.utils.CryptoUtils cryptoUtils) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.cryptoUtils = cryptoUtils;
    }

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Wrap request/response to cache content if needed (for body inspection later)
        // Note: For now we just log metadata, but wrapping is good practice for future
        // body logging
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logTransaction(wrappedRequest, wrappedResponse, duration);

            // IMPORTANT: Copy content back to response
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logTransaction(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response,
            long durationMs) {
        try {
            Map<String, Object> logEntry = new java.util.LinkedHashMap<>();

            // 1. Root Fields
            logEntry.put("timestamp", java.time.Instant.now().toString());
            logEntry.put("level", response.getStatus() >= 400 ? "WARN" : "INFO");
            logEntry.put("component", "NIS2-SHIELD-JAVA");
            logEntry.put("event_id", "HTTP_ACCESS");

            // 2. Request
            Map<String, Object> reqMap = new java.util.LinkedHashMap<>();
            String ip = request.getRemoteAddr();
            if (properties.getLogging().isAnonymizeIp()) {
                ip = anonymizeIp(ip);
            }
            reqMap.put("method", request.getMethod());
            reqMap.put("url", request.getRequestURI());
            reqMap.put("ip", ip);
            reqMap.put("user_agent", request.getHeader("User-Agent"));
            logEntry.put("request", reqMap);

            // 3. Response
            Map<String, Object> resMap = new java.util.LinkedHashMap<>();
            resMap.put("status", response.getStatus());
            resMap.put("duration_ms", durationMs);
            logEntry.put("response", resMap);

            // 4. User (Optional placeholder)
            // logEntry.put("user", ...);

            // Encrypt PII if enabled (This modifies logEntry in place)
            if (properties.getLogging().isEncryptPii()) {
                encryptPiiFields(logEntry);
            }

            // Serialize to JSON for signing
            String jsonLog = objectMapper.writeValueAsString(logEntry);

            // 5. Integrity Sign
            String signature = signLog(jsonLog);

            // 6. Final Object usually includes the hash INSIDE or OUTSIDE.
            // The schema says `integrity_hash` is a field. We must add it to the map.
            // BUT: Standard practice is hash covers the *content*, so we add it after
            // signing.
            // However, Jackson ObjectMapper doesn't guarantee order unless LinkedHashMap is
            // used everywhere.
            // To be safe and compliant with the "Schema Definition" which shows the hash
            // INSIDE the JSON:

            // Re-parse or just append if we were doing manual string building.
            // Better: Add to map, BUT signature verification needs to know to remove it.
            // For now, per existing patterns, we'll keep the signature separated in the
            // final output
            // OR we include it in the object.
            // Let's stick to the previous pattern: "log" (content) + "integrity_hash"
            // WAIT - The NEW schema request shows integrity_hash INSIDE the root structure.
            // "integrity_hash": "base64(hmac-sha256(json_string_excluding_hash))"

            Map<String, Object> finalLogObj = objectMapper.readValue(jsonLog,
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                    });
            finalLogObj.put("integrity_hash", signature);

            auditLogger.info(objectMapper.writeValueAsString(finalLogObj));

        } catch (Exception e) {
            // Fallback safe logging
            auditLogger.error("FAILED_TO_LOG_NIS2_ENTRY: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void encryptPiiFields(Map<String, Object> data) {
        // Recursive encryption of PII fields
        // For simplicity, we define a static list of keys to encrypt.
        // In prod this could be configurable.
        String[] sensitiveKeys = { "user_id", "email", "username", "ip", "user_agent" };

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                encryptPiiFields((Map<String, Object>) value);
            } else if (value instanceof String) {
                for (String sensitiveKey : sensitiveKeys) {
                    if (key.equalsIgnoreCase(sensitiveKey)) {
                        entry.setValue("[ENCRYPTED]" + cryptoUtils.encrypt((String) value));
                        break;
                    }
                }
            }
        }
    }

    private String anonymizeIp(String ip) {
        if (ip == null)
            return "unknown";
        // Simple anonymization: mask last octet
        int lastDot = ip.lastIndexOf('.');
        if (lastDot != -1) {
            return ip.substring(0, lastDot) + ".0";
        }
        return ip;
    }

    private String signLog(String content) {
        String key = properties.getIntegrityKey();
        if (key == null || key.isEmpty())
            return "unsigned";

        try {
            Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256_HMAC.init(secret_key);
            return Base64.getEncoder().encodeToString(sha256_HMAC.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            return "signing-error";
        }
    }
}
