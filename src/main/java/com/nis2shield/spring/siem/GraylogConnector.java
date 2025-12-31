package com.nis2shield.spring.siem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Graylog GELF connector.
 * Sends audit logs to Graylog using the GELF (Graylog Extended Log Format) over HTTP.
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * nis2:
 *   siem:
 *     graylog:
 *       enabled: true
 *       url: http://graylog.example.com:12201/gelf
 *       facility: nis2-spring-shield
 * </pre>
 */
public class GraylogConnector implements SiemConnector {

    private static final Logger log = LoggerFactory.getLogger(GraylogConnector.class);
    
    private final String url;
    private final String facility;
    private final String host;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GraylogConnector(String url, String facility) {
        this.url = url;
        this.facility = facility != null ? facility : "nis2-spring-shield";
        this.host = getHostname();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "Graylog GELF";
    }

    @Override
    public void send(Map<String, Object> auditLog) {
        try {
            // Convert to GELF format
            Map<String, Object> gelfMessage = new HashMap<>();
            gelfMessage.put("version", "1.1");
            gelfMessage.put("host", host);
            gelfMessage.put("facility", facility);
            gelfMessage.put("timestamp", System.currentTimeMillis() / 1000.0);
            
            // Main message
            String method = String.valueOf(auditLog.getOrDefault("method", ""));
            String path = String.valueOf(auditLog.getOrDefault("path", ""));
            gelfMessage.put("short_message", method + " " + path);
            
            // Level: INFO=6, WARN=4, ERROR=3
            int riskScore = (int) auditLog.getOrDefault("risk_score", 0);
            int level = riskScore > 50 ? 3 : (riskScore > 20 ? 4 : 6);
            gelfMessage.put("level", level);
            
            // Add all audit fields as GELF additional fields (prefixed with _)
            for (Map.Entry<String, Object> entry : auditLog.entrySet()) {
                gelfMessage.put("_" + entry.getKey(), entry.getValue());
            }

            String body = objectMapper.writeValueAsString(gelfMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 202) {
                log.warn("Graylog returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send GELF event to Graylog: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return url != null && !url.isEmpty();
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
