package com.nis2shield.spring.compliance;

import com.nis2shield.spring.configuration.Nis2Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Nis2ComplianceChecker.
 */
class Nis2ComplianceCheckerTest {

    private Nis2Properties properties;

    @BeforeEach
    void setUp() {
        properties = new Nis2Properties();
    }

    @Test
    @DisplayName("Should return 100% score with full configuration")
    void fullConfigurationShouldPass() {
        // Configure all required settings
        properties.setEnabled(true);
        properties.setIntegrityKey("a-very-long-integrity-key-of-32-chars!");
        properties.setEncryptionKey("16-char-key-aes!"); // 16 chars for AES-128
        
        properties.getActiveDefense().setRateLimitEnabled(true);
        properties.getActiveDefense().setRateLimitCapacity(100);
        
        properties.getSecurityHeaders().setEnabled(true);
        properties.getSecurityHeaders().setHstsEnabled(true);
        properties.getSecurityHeaders().setCspEnabled(true);
        
        properties.getLogging().setEnabled(true);
        properties.getLogging().setAnonymizeIp(true);
        properties.getLogging().setEncryptPii(true);
        
        properties.getSession().setGuardEnabled(true);
        
        properties.getSiem().getSplunk().setEnabled(true);
        
        Nis2ComplianceChecker checker = new Nis2ComplianceChecker(properties);
        ComplianceReport report = checker.runAudit();
        
        assertEquals(100, report.getScore());
        assertEquals(8, report.getPassed());
        assertEquals(0, report.getFailed());
        assertEquals(0, report.getWarnings());
    }

    @Test
    @DisplayName("Should detect missing encryption key as FAIL")
    void missingEncryptionKeyShouldFail() {
        properties.setEnabled(true);
        properties.setIntegrityKey("a-very-long-integrity-key-of-32-chars!");
        // No encryption key set
        
        Nis2ComplianceChecker checker = new Nis2ComplianceChecker(properties);
        ComplianceReport report = checker.runAudit();
        
        assertTrue(report.getFailed() > 0, "Should have at least one failure");
        
        boolean hasEncryptionFail = report.getChecks().stream()
                .anyMatch(c -> c.name().equals("Encryption Key") 
                        && c.status() == ComplianceReport.CheckStatus.FAIL);
        assertTrue(hasEncryptionFail, "Should fail encryption key check");
    }

    @Test
    @DisplayName("Should detect disabled shield as FAIL")
    void disabledShieldShouldFail() {
        properties.setEnabled(false);
        
        Nis2ComplianceChecker checker = new Nis2ComplianceChecker(properties);
        ComplianceReport report = checker.runAudit();
        
        boolean hasShieldFail = report.getChecks().stream()
                .anyMatch(c -> c.name().equals("NIS2 Shield Enabled") 
                        && c.status() == ComplianceReport.CheckStatus.FAIL);
        assertTrue(hasShieldFail, "Should fail shield enabled check");
    }

    @Test
    @DisplayName("Should return WARN for short integrity key")
    void shortIntegrityKeyShouldWarn() {
        properties.setEnabled(true);
        properties.setIntegrityKey("short"); // Less than 32 chars
        
        Nis2ComplianceChecker checker = new Nis2ComplianceChecker(properties);
        ComplianceReport report = checker.runAudit();
        
        boolean hasIntegrityWarn = report.getChecks().stream()
                .anyMatch(c -> c.name().equals("Integrity Key") 
                        && c.status() == ComplianceReport.CheckStatus.WARN);
        assertTrue(hasIntegrityWarn, "Should warn about short integrity key");
    }

    @Test
    @DisplayName("Should return correct NIS2 article references")
    void shouldHaveCorrectArticleReferences() {
        Nis2ComplianceChecker checker = new Nis2ComplianceChecker(properties);
        ComplianceReport report = checker.runAudit();
        
        // Verify specific articles
        assertTrue(report.getChecks().stream()
                .anyMatch(c -> c.name().equals("Encryption Key") && c.nis2Article().equals("Art. 21.2.f")));
        assertTrue(report.getChecks().stream()
                .anyMatch(c -> c.name().equals("Rate Limiting") && c.nis2Article().equals("Art. 21.2.e")));
        assertTrue(report.getChecks().stream()
                .anyMatch(c -> c.name().equals("Forensic Logging") && c.nis2Article().equals("Art. 21.2.h")));
    }
}
