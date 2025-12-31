package com.nis2shield.spring.siem;

import java.util.Map;

/**
 * Interface for SIEM connectors.
 * Implementations send audit logs to various SIEM systems.
 */
public interface SiemConnector {

    /**
     * Get the name of this SIEM connector.
     */
    String getName();

    /**
     * Send an audit log event to the SIEM system.
     *
     * @param auditLog The audit log as a map of key-value pairs
     */
    void send(Map<String, Object> auditLog);

    /**
     * Check if this connector is enabled and properly configured.
     */
    boolean isEnabled();
}
