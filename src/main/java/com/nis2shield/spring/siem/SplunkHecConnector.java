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
 * Splunk HTTP Event Collector (HEC) connector.
 * Sends audit logs directly to Splunk via the HEC endpoint.
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * nis2:
 *   siem:
 *     splunk:
 *       enabled: true
 *       url: https://splunk.example.com:8088/services/collector/event
 *       token: your-hec-token
 *       index: security
 *       source: nis2-spring-shield
 * </pre>
 */
public class SplunkHecConnector implements SiemConnector {

    private static final Logger log = LoggerFactory.getLogger(SplunkHecConnector.class);
    
    private final String url;
    private final String token;
    private final String index;
    private final String source;
    private final String sourceType;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public SplunkHecConnector(String url, String token, String index, String source) {
        this.url = url;
        this.token = token;
        this.index = index != null ? index : "main";
        this.source = source != null ? source : "nis2-spring-shield";
        this.sourceType = "_json";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String getName() {
        return "Splunk HEC";
    }

    @Override
    public void send(Map<String, Object> auditLog) {
        try {
            // Wrap in Splunk HEC event format
            Map<String, Object> hecEvent = Map.of(
                "time", System.currentTimeMillis() / 1000.0,
                "source", source,
                "sourcetype", sourceType,
                "index", index,
                "event", auditLog
            );

            String body = objectMapper.writeValueAsString(hecEvent);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Splunk " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Splunk HEC returned status {}: {}", response.statusCode(), response.body());
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to send event to Splunk HEC: {}", e.getMessage());
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return url != null && !url.isEmpty() && token != null && !token.isEmpty();
    }
}
