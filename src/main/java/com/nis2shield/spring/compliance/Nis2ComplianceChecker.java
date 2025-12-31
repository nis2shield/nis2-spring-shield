package com.nis2shield.spring.compliance;

import com.nis2shield.spring.configuration.Nis2Properties;
import org.springframework.stereotype.Service;

/**
 * Service that checks the application configuration against NIS2 requirements.
 * Equivalent to Django's check_nis2 management command.
 */
@Service
public class Nis2ComplianceChecker {

    private final Nis2Properties properties;
    private final String appName;

    public Nis2ComplianceChecker(Nis2Properties properties) {
        this.properties = properties;
        this.appName = "Spring Boot Application";
    }

    /**
     * Runs all compliance checks and returns a report.
     * 
     * @return ComplianceReport with all check results
     */
    public ComplianceReport runAudit() {
        ComplianceReport report = new ComplianceReport(appName);

        // Check 1: NIS2 Shield Enabled
        report.addCheck(checkShieldEnabled());

        // Check 2: Integrity Key (HMAC signing)
        report.addCheck(checkIntegrityKey());

        // Check 3: Encryption Key (AES for PII)
        report.addCheck(checkEncryptionKey());

        // Check 4: Rate Limiting
        report.addCheck(checkRateLimiting());

        // Check 5: Security Headers
        report.addCheck(checkSecurityHeaders());

        // Check 6: Forensic Logging
        report.addCheck(checkLogging());

        // Check 7: Session Guard
        report.addCheck(checkSessionGuard());

        // Check 8: SIEM Integration
        report.addCheck(checkSiemIntegration());

        return report;
    }

    private ComplianceReport.CheckResult checkShieldEnabled() {
        boolean enabled = properties.isEnabled();
        return new ComplianceReport.CheckResult(
                "NIS2 Shield Enabled",
                "NIS2 Shield middleware is active",
                enabled ? ComplianceReport.CheckStatus.PASS : ComplianceReport.CheckStatus.FAIL,
                "General"
        );
    }

    private ComplianceReport.CheckResult checkIntegrityKey() {
        String key = properties.getIntegrityKey();
        boolean valid = key != null && key.length() >= 32;
        ComplianceReport.CheckStatus status;
        if (valid) {
            status = ComplianceReport.CheckStatus.PASS;
        } else if (key != null && !key.isEmpty()) {
            status = ComplianceReport.CheckStatus.WARN; // Key exists but too short
        } else {
            status = ComplianceReport.CheckStatus.FAIL;
        }
        return new ComplianceReport.CheckResult(
                "Integrity Key",
                "HMAC signing key for log integrity (min 32 chars)",
                status,
                "Art. 21.2.h"
        );
    }

    private ComplianceReport.CheckResult checkEncryptionKey() {
        String key = properties.getEncryptionKey();
        boolean valid = key != null && (key.length() == 16 || key.length() == 24 || key.length() == 32);
        ComplianceReport.CheckStatus status;
        if (valid) {
            status = ComplianceReport.CheckStatus.PASS;
        } else if (key != null && !key.isEmpty()) {
            status = ComplianceReport.CheckStatus.WARN; // Key exists but wrong size
        } else {
            status = ComplianceReport.CheckStatus.FAIL;
        }
        return new ComplianceReport.CheckResult(
                "Encryption Key",
                "AES encryption key for PII (16/24/32 chars for AES-128/192/256)",
                status,
                "Art. 21.2.f"
        );
    }

    private ComplianceReport.CheckResult checkRateLimiting() {
        boolean enabled = properties.getActiveDefense().isRateLimitEnabled();
        long capacity = properties.getActiveDefense().getRateLimitCapacity();
        ComplianceReport.CheckStatus status;
        if (!enabled) {
            status = ComplianceReport.CheckStatus.FAIL;
        } else if (capacity > 1000) {
            status = ComplianceReport.CheckStatus.WARN; // Enabled but very high limit
        } else {
            status = ComplianceReport.CheckStatus.PASS;
        }
        return new ComplianceReport.CheckResult(
                "Rate Limiting",
                "Protection against DoS attacks",
                status,
                "Art. 21.2.e"
        );
    }

    private ComplianceReport.CheckResult checkSecurityHeaders() {
        boolean enabled = properties.getSecurityHeaders().isEnabled();
        boolean hsts = properties.getSecurityHeaders().isHstsEnabled();
        boolean csp = properties.getSecurityHeaders().isCspEnabled();
        
        ComplianceReport.CheckStatus status;
        if (enabled && hsts && csp) {
            status = ComplianceReport.CheckStatus.PASS;
        } else if (enabled) {
            status = ComplianceReport.CheckStatus.WARN; // Enabled but some headers missing
        } else {
            status = ComplianceReport.CheckStatus.FAIL;
        }
        return new ComplianceReport.CheckResult(
                "Security Headers",
                "HSTS, CSP, X-Content-Type-Options, etc.",
                status,
                "Art. 21.2.d"
        );
    }

    private ComplianceReport.CheckResult checkLogging() {
        boolean enabled = properties.getLogging().isEnabled();
        boolean anonymize = properties.getLogging().isAnonymizeIp();
        boolean encryptPii = properties.getLogging().isEncryptPii();
        
        ComplianceReport.CheckStatus status;
        if (enabled && anonymize && encryptPii) {
            status = ComplianceReport.CheckStatus.PASS;
        } else if (enabled) {
            status = ComplianceReport.CheckStatus.WARN; // Logging but no privacy measures
        } else {
            status = ComplianceReport.CheckStatus.FAIL;
        }
        return new ComplianceReport.CheckResult(
                "Forensic Logging",
                "Audit logging with PII protection",
                status,
                "Art. 21.2.h"
        );
    }

    private ComplianceReport.CheckResult checkSessionGuard() {
        boolean enabled = properties.getSession().isGuardEnabled();
        return new ComplianceReport.CheckResult(
                "Session Guard",
                "Session hijacking protection",
                enabled ? ComplianceReport.CheckStatus.PASS : ComplianceReport.CheckStatus.WARN,
                "Art. 21.2.d"
        );
    }

    private ComplianceReport.CheckResult checkSiemIntegration() {
        boolean anyEnabled = properties.getSiem().getSplunk().isEnabled()
                || properties.getSiem().getDatadog().isEnabled()
                || properties.getSiem().getQradar().isEnabled()
                || properties.getSiem().getGraylog().isEnabled();
        
        return new ComplianceReport.CheckResult(
                "SIEM Integration",
                "Connection to Security Information and Event Management",
                anyEnabled ? ComplianceReport.CheckStatus.PASS : ComplianceReport.CheckStatus.WARN,
                "Art. 21.2.b"
        );
    }
}
