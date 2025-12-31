package com.nis2shield.spring.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Webhook notifier for security events.
 * Sends real-time notifications to external webhooks (Slack, Teams, Discord, etc.)
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * nis2:
 *   notifications:
 *     webhook:
 *       enabled: true
 *       url: https://hooks.slack.com/services/YOUR/WEBHOOK/URL
 *       events:
 *         - RATE_LIMIT_EXCEEDED
 *         - IP_BLOCKED
 *         - TOR_BLOCKED
 *         - HIGH_RISK_REQUEST
 *       retryAttempts: 3
 * </pre>
 */
public class WebhookNotifier {

    private static final Logger log = LoggerFactory.getLogger(WebhookNotifier.class);

    public enum EventType {
        RATE_LIMIT_EXCEEDED,
        IP_BLOCKED,
        TOR_BLOCKED,
        GEO_BLOCKED,
        HIGH_RISK_REQUEST,
        SECURITY_HEADER_BLOCKED,
        AUDIT_LOG
    }

    private final String webhookUrl;
    private final List<EventType> enabledEvents;
    private final int retryAttempts;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final BlockingQueue<WebhookPayload> queue;
    private final ExecutorService executor;
    private volatile boolean running = true;

    public WebhookNotifier(String webhookUrl, List<EventType> enabledEvents, int retryAttempts) {
        this.webhookUrl = webhookUrl;
        this.enabledEvents = enabledEvents != null ? enabledEvents : List.of(
            EventType.RATE_LIMIT_EXCEEDED,
            EventType.IP_BLOCKED,
            EventType.TOR_BLOCKED,
            EventType.HIGH_RISK_REQUEST
        );
        this.retryAttempts = retryAttempts > 0 ? retryAttempts : 3;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
        this.queue = new LinkedBlockingQueue<>(1000);
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "nis2-webhook-notifier");
            t.setDaemon(true);
            return t;
        });
        
        // Start async processing
        this.executor.submit(this::processQueue);
    }

    /**
     * Send a notification (non-blocking, queued).
     */
    public void notify(EventType eventType, String ip, String path, String method, String message, Map<String, Object> metadata) {
        if (!enabledEvents.contains(eventType)) {
            return;
        }

        WebhookPayload payload = new WebhookPayload(
            eventType,
            Instant.now().toString(),
            ip,
            path,
            method,
            message,
            metadata
        );

        if (!queue.offer(payload)) {
            log.warn("Webhook notification queue is full, dropping event: {}", eventType);
        }
    }

    /**
     * Process queued notifications.
     */
    private void processQueue() {
        while (running) {
            try {
                WebhookPayload payload = queue.take();
                sendWithRetry(payload);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Send with retry logic.
     */
    private void sendWithRetry(WebhookPayload payload) {
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                send(payload);
                return;
            } catch (Exception e) {
                if (attempt < retryAttempts) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt - 1) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    log.error("Failed to send webhook after {} attempts: {}", retryAttempts, e.getMessage());
                }
            }
        }
    }

    /**
     * Send the actual HTTP request.
     */
    private void send(WebhookPayload payload) throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
            "event", payload.eventType.name(),
            "timestamp", payload.timestamp,
            "ip", payload.ip,
            "path", payload.path,
            "method", payload.method,
            "message", payload.message,
            "metadata", payload.metadata != null ? payload.metadata : Map.of()
        );

        String jsonBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .header("User-Agent", "NIS2Shield-SpringShield/0.2.0")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            throw new IOException("Webhook returned status " + response.statusCode());
        }
    }

    public boolean isEnabled() {
        return webhookUrl != null && !webhookUrl.isEmpty();
    }

    public void shutdown() {
        running = false;
        executor.shutdownNow();
    }

    private record WebhookPayload(
        EventType eventType,
        String timestamp,
        String ip,
        String path,
        String method,
        String message,
        Map<String, Object> metadata
    ) {}
}
