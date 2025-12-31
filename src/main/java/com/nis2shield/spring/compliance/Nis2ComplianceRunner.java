package com.nis2shield.spring.compliance;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;

/**
 * ApplicationRunner that executes NIS2 compliance audit when --check-nis2 argument is passed.
 * Equivalent to Django's `python manage.py check_nis2` command.
 * 
 * Usage:
 *   java -jar myapp.jar --check-nis2
 */
@Order(1)
public class Nis2ComplianceRunner implements ApplicationRunner {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_YELLOW = "\u001B[33m";
    private static final String ANSI_BOLD = "\u001B[1m";

    private final Nis2ComplianceChecker complianceChecker;

    public Nis2ComplianceRunner(Nis2ComplianceChecker complianceChecker) {
        this.complianceChecker = complianceChecker;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("check-nis2") && !args.getNonOptionArgs().contains("--check-nis2")) {
            return; // Not requested, skip
        }

        ComplianceReport report = complianceChecker.runAudit();
        printReport(report);

        // Exit with non-zero code if there are failures
        if (report.getFailed() > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private void printReport(ComplianceReport report) {
        System.out.println();
        System.out.println(ANSI_BOLD + "[NIS2 SHIELD AUDIT REPORT]" + ANSI_RESET);
        System.out.println("Application: " + report.getAppName());
        System.out.println("Generated: " + report.getGeneratedAt());
        System.out.println("------------------------------------------------");

        for (ComplianceReport.CheckResult check : report.getChecks()) {
            String statusStr = switch (check.status()) {
                case PASS -> ANSI_GREEN + "[PASS]" + ANSI_RESET;
                case FAIL -> ANSI_RED + "[FAIL]" + ANSI_RESET;
                case WARN -> ANSI_YELLOW + "[WARN]" + ANSI_RESET;
            };
            String article = check.nis2Article().isEmpty() ? "" : " (" + check.nis2Article() + ")";
            System.out.println(statusStr + " " + check.name() + article);
            System.out.println("       " + check.description());
        }

        System.out.println("------------------------------------------------");
        System.out.println("PASSED: " + report.getPassed() + " | WARNINGS: " + report.getWarnings() + " | FAILED: " + report.getFailed());
        
        int score = report.getScore();
        String scoreColor = score >= 80 ? ANSI_GREEN : (score >= 50 ? ANSI_YELLOW : ANSI_RED);
        System.out.println(ANSI_BOLD + "COMPLIANCE SCORE: " + scoreColor + score + "/100" + ANSI_RESET);

        if (report.getFailed() > 0) {
            System.out.println();
            System.out.println(ANSI_RED + "⚠ Action Required: Fix the failed checks above for NIS2 compliance." + ANSI_RESET);
        } else if (report.getWarnings() > 0) {
            System.out.println();
            System.out.println(ANSI_YELLOW + "ℹ Recommendations: Consider addressing the warnings for full compliance." + ANSI_RESET);
        } else {
            System.out.println();
            System.out.println(ANSI_GREEN + "✓ Excellent! Your application meets all NIS2 requirements." + ANSI_RESET);
        }
        System.out.println();
    }
}
