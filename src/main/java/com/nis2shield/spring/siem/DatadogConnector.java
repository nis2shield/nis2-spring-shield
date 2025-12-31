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
import java.util.Map;

/**
 * Datadog Log Intake connector.
 * Sends audit logs directly to Datadog via the Logs API.
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * nis2:
 *   siem:
 *     datadog:
 *       enabled: true
 *       apiKey: your-api-key
 *       site: datadoghq.eu  # or datadoghq.com
 *       service: nis2-spring-shield
 *       source: spring
 * </pre>
 */
public class DatadogConnector implements SiemConnector {

    private static final Logger log = LoggerFactory.getLogger(DatadogConnector.class);
    
    private final String apiKey;
    private final String site;
    private final String service;
    private final String source;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DatadogConnector(String apiKey, String site, String service, String source) {
        this.apiKey = apiKey;
        this.site = site != null ? site : "datadoghq.com";
        this.service = service != null ? service : "nis2-spring-shield";
        this.source = source != null ? source : "spring";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "Datadog";
    }

    @Override
    public void send(Map<String, Object> auditLog) {
        try {
            // Add Datadog-specific fields
            auditLog.put("ddsource", source);
            auditLog.put("ddtags", "env:production,service:" + service);
            auditLog.put("service", service);
            auditLog.put("hostname", getHostname());

            String body = objectMapper.writeValueAsString(auditLog);
            String url = String.format("https://http-intake.logs.%s/api/v2/logs", site);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("DD-API-KEY", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 && response.statusCode() != 202) {
                log.warn("Datadog returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send event to Datadog: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty();
    }

    private String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
