package com.nis2shield.spring.siem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * IBM QRadar connector using SYSLOG with CEF (Common Event Format).
 * Sends audit logs to QRadar via UDP syslog.
 *
 * <p>Configuration example in application.yml:</p>
 * <pre>
 * nis2:
 *   siem:
 *     qradar:
 *       enabled: true
 *       host: qradar.example.com
 *       port: 514
 *       deviceVendor: NIS2Shield
 *       deviceProduct: SpringShield
 * </pre>
 */
public class QRadarConnector implements SiemConnector {

    private static final Logger log = LoggerFactory.getLogger(QRadarConnector.class);
    
    private final String host;
    private final int port;
    private final String deviceVendor;
    private final String deviceProduct;
    private final String deviceVersion;
    private final ObjectMapper objectMapper;
    private DatagramSocket socket;

    public QRadarConnector(String host, int port, String deviceVendor, String deviceProduct) {
        this.host = host;
        this.port = port > 0 ? port : 514;
        this.deviceVendor = deviceVendor != null ? deviceVendor : "NIS2Shield";
        this.deviceProduct = deviceProduct != null ? deviceProduct : "SpringShield";
        this.deviceVersion = "0.2.0";
        this.objectMapper = new ObjectMapper();
        
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            log.error("Failed to create UDP socket for QRadar: {}", e.getMessage());
        }
    }

    @Override
    public String getName() {
        return "QRadar CEF";
    }

    @Override
    public void send(Map<String, Object> auditLog) {
        if (socket == null) return;

        try {
            String cefMessage = buildCefMessage(auditLog);
            byte[] data = cefMessage.getBytes(StandardCharsets.UTF_8);
            
            InetAddress address = InetAddress.getByName(host);
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            socket.send(packet);
            
        } catch (IOException e) {
            log.error("Failed to send CEF event to QRadar: {}", e.getMessage());
        }
    }

    /**
     * Build a CEF (Common Event Format) message.
     * Format: CEF:Version|Device Vendor|Device Product|Device Version|Signature ID|Name|Severity|Extension
     */
    private String buildCefMessage(Map<String, Object> auditLog) {
        String method = String.valueOf(auditLog.getOrDefault("method", "UNKNOWN"));
        String path = String.valueOf(auditLog.getOrDefault("path", "/"));
        String ip = String.valueOf(auditLog.getOrDefault("ip", "0.0.0.0"));
        int statusCode = (int) auditLog.getOrDefault("status_code", 200);
        int riskScore = (int) auditLog.getOrDefault("risk_score", 0);
        
        // Map risk score to CEF severity (0-10 scale)
        int severity = Math.min(riskScore, 10);
        
        // Signature ID based on event type
        String signatureId = statusCode >= 400 ? "HTTP_ERROR" : "HTTP_REQUEST";
        String eventName = method + " " + path;

        // Build extension fields
        String extension = String.format(
            "src=%s dpt=%d request=%s requestMethod=%s cs1Label=StatusCode cs1=%d cs2Label=RiskScore cs2=%d rt=%s",
            ip, port, path, method, statusCode, riskScore, Instant.now().toString()
        );

        return String.format(
            "CEF:0|%s|%s|%s|%s|%s|%d|%s",
            deviceVendor, deviceProduct, deviceVersion,
            signatureId, eventName, severity, extension
        );
    }

    @Override
    public boolean isEnabled() {
        return host != null && !host.isEmpty() && socket != null;
    }

    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
