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
import java.util.HashMap;
import java.util.Map;

/**
 * NIS2 compliance auditing filter for Spring Boot applications.
 * Intercepts all HTTP requests, logs them in JSON format with HMAC-SHA256 integrity
 * signatures, and optionally encrypts PII fields.
 *
 * <p>The filter is auto-configured when nis2-spring-shield is on the classpath.</p>
 *
 * <p>Example configuration in application.yml:</p>
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

    public Nis2AuditingFilter(Nis2Properties properties, ObjectMapper objectMapper, com.nis2shield.spring.utils.CryptoUtils cryptoUtils) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.cryptoUtils = cryptoUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Wrap request/response to cache content if needed (for body inspection later)
        // Note: For now we just log metadata, but wrapping is good practice for future body logging
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

    private void logTransaction(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long durationMs) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            
            // WHO
            String ip = request.getRemoteAddr();
            if (properties.getLogging().isAnonymizeIp()) {
                ip = anonymizeIp(ip);
            }
            
            Map<String, Object> who = new HashMap<>();
            who.put("ip", ip);
            who.put("user_agent", request.getHeader("User-Agent"));
            // User ID would go here if Spring Security is integrated
            
            // WHAT
            Map<String, Object> what = new HashMap<>();
            what.put("method", request.getMethod());
            what.put("url", request.getRequestURI());
            
            // RESULT
            Map<String, Object> result = new HashMap<>();
            result.put("status", response.getStatus());
            result.put("duration_seconds", durationMs / 1000.0);
            
            logEntry.put("who", who);
            logEntry.put("what", what);
            logEntry.put("result", result);
            logEntry.put("timestamp", java.time.Instant.now().toString());

            // Encrypt PII if enabled
            if (properties.getLogging().isEncryptPii()) {
                encryptPiiFields(logEntry);
            }
            
            String jsonLog = objectMapper.writeValueAsString(logEntry);
            
            // Integrity Sign
            String signature = signLog(jsonLog);
            
            Map<String, Object> finalLog = new HashMap<>();
            finalLog.put("log", logEntry);
            finalLog.put("integrity_hash", signature);
            
            auditLogger.info(objectMapper.writeValueAsString(finalLog));
            
        } catch (Exception e) {
            logger.error("Failed to log NIS2 audit entry", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void encryptPiiFields(Map<String, Object> data) {
        // Recursive encryption of PII fields
        // For simplicity, we define a static list of keys to encrypt. 
        // In prod this could be configurable.
        String[] sensitiveKeys = {"user_id", "email", "username", "ip", "user_agent"};
        
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
        if (ip == null) return "unknown";
        // Simple anonymization: mask last octet
        int lastDot = ip.lastIndexOf('.');
        if (lastDot != -1) {
            return ip.substring(0, lastDot) + ".0";
        }
        return ip;
    }

    private String signLog(String content) {
        String key = properties.getIntegrityKey();
        if (key == null || key.isEmpty()) return "unsigned";
        
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
