package com.nis2shield.spring.compliance;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO representing a NIS2 compliance report.
 * Contains the results of all compliance checks.
 */
public class ComplianceReport {

    private final Instant generatedAt;
    private final String appName;
    private final List<CheckResult> checks;
    private int passed;
    private int failed;
    private int warnings;

    public ComplianceReport(String appName) {
        this.generatedAt = Instant.now();
        this.appName = appName;
        this.checks = new ArrayList<>();
        this.passed = 0;
        this.failed = 0;
        this.warnings = 0;
    }

    public void addCheck(CheckResult check) {
        this.checks.add(check);
        switch (check.status()) {
            case PASS -> passed++;
            case FAIL -> failed++;
            case WARN -> warnings++;
        }
    }

    public int getScore() {
        int total = checks.size();
        if (total == 0) return 100;
        return (int) ((passed * 100.0) / total);
    }

    public Instant getGeneratedAt() { return generatedAt; }
    public String getAppName() { return appName; }
    public List<CheckResult> getChecks() { return checks; }
    public int getPassed() { return passed; }
    public int getFailed() { return failed; }
    public int getWarnings() { return warnings; }

    /**
     * Represents a single compliance check result.
     */
    public record CheckResult(
            String name,
            String description,
            CheckStatus status,
            String nis2Article
    ) {}

    public enum CheckStatus {
        PASS, FAIL, WARN
    }
}
