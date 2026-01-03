package com.nis2shield.spring.compliance;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Service for generating compliance reports in various formats.
 * Supports PDF (via OpenPDF when available), HTML, and JSON.
 */
public class ComplianceReportService {

    private final Nis2ComplianceChecker complianceChecker;

    public ComplianceReportService(Nis2ComplianceChecker complianceChecker) {
        this.complianceChecker = complianceChecker;
    }

    /**
     * Generate a compliance report and return as JSON string.
     */
    public String generateJson() {
        ComplianceReport report = complianceChecker.runAudit();
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"generatedAt\": \"").append(report.getGeneratedAt()).append("\",\n");
        json.append("  \"appName\": \"").append(report.getAppName()).append("\",\n");
        json.append("  \"score\": ").append(report.getScore()).append(",\n");
        json.append("  \"passed\": ").append(report.getPassed()).append(",\n");
        json.append("  \"failed\": ").append(report.getFailed()).append(",\n");
        json.append("  \"warnings\": ").append(report.getWarnings()).append(",\n");
        json.append("  \"checks\": [\n");

        var checks = report.getChecks();
        for (int i = 0; i < checks.size(); i++) {
            var check = checks.get(i);
            json.append("    {\n");
            json.append("      \"name\": \"").append(check.name()).append("\",\n");
            json.append("      \"description\": \"").append(check.description()).append("\",\n");
            json.append("      \"status\": \"").append(check.status()).append("\",\n");
            json.append("      \"nis2Article\": \"").append(check.nis2Article()).append("\"\n");
            json.append("    }");
            if (i < checks.size() - 1)
                json.append(",");
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}");
        return json.toString();
    }

    /**
     * Generate a compliance report as HTML.
     */
    public String generateHtml() {
        ComplianceReport report = complianceChecker.runAudit();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>NIS2 Compliance Report - ").append(report.getAppName()).append("</title>\n");
        html.append("  <style>\n");
        html.append(
                "    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; background: #f5f5f5; }\n");
        html.append(
                "    .report { background: white; border-radius: 8px; padding: 24px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    h1 { color: #1a1a2e; border-bottom: 2px solid #f97316; padding-bottom: 10px; }\n");
        html.append(
                "    .score { font-size: 48px; font-weight: bold; text-align: center; padding: 20px; border-radius: 8px; margin: 20px 0; }\n");
        html.append("    .score.pass { background: #dcfce7; color: #166534; }\n");
        html.append("    .score.warn { background: #fef3c7; color: #92400e; }\n");
        html.append("    .score.fail { background: #fee2e2; color: #991b1b; }\n");
        html.append("    .checks { margin-top: 20px; }\n");
        html.append(
                "    .check { display: flex; align-items: flex-start; padding: 12px; border-bottom: 1px solid #eee; }\n");
        html.append("    .check:last-child { border-bottom: none; }\n");
        html.append("    .status { width: 60px; font-weight: bold; flex-shrink: 0; }\n");
        html.append("    .status.PASS { color: #16a34a; }\n");
        html.append("    .status.WARN { color: #d97706; }\n");
        html.append("    .status.FAIL { color: #dc2626; }\n");
        html.append("    .check-info { flex: 1; }\n");
        html.append("    .check-name { font-weight: 600; }\n");
        html.append("    .check-desc { color: #666; font-size: 14px; margin-top: 4px; }\n");
        html.append(
                "    .article { color: #f97316; font-size: 12px; background: #fff7ed; padding: 2px 8px; border-radius: 4px; margin-left: 8px; }\n");
        html.append("    .summary { display: flex; gap: 20px; justify-content: center; margin: 20px 0; }\n");
        html.append("    .summary-item { text-align: center; padding: 10px 20px; border-radius: 8px; }\n");
        html.append("    .summary-item.passed { background: #dcfce7; }\n");
        html.append("    .summary-item.warnings { background: #fef3c7; }\n");
        html.append("    .summary-item.failed { background: #fee2e2; }\n");
        html.append("    .summary-num { font-size: 24px; font-weight: bold; }\n");
        html.append("    .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class=\"report\">\n");
        html.append("    <h1>🛡️ NIS2 Compliance Report</h1>\n");
        html.append("    <p><strong>Application:</strong> ").append(report.getAppName()).append("</p>\n");
        html.append("    <p><strong>Generated:</strong> ").append(report.getGeneratedAt()).append("</p>\n");

        // Score
        int score = report.getScore();
        String scoreClass = score >= 80 ? "pass" : (score >= 50 ? "warn" : "fail");
        html.append("    <div class=\"score ").append(scoreClass).append("\">\n");
        html.append("      ").append(score).append("/100\n");
        html.append("    </div>\n");

        // Summary
        html.append("    <div class=\"summary\">\n");
        html.append("      <div class=\"summary-item passed\"><div class=\"summary-num\">").append(report.getPassed())
                .append("</div>Passed</div>\n");
        html.append("      <div class=\"summary-item warnings\"><div class=\"summary-num\">")
                .append(report.getWarnings()).append("</div>Warnings</div>\n");
        html.append("      <div class=\"summary-item failed\"><div class=\"summary-num\">").append(report.getFailed())
                .append("</div>Failed</div>\n");
        html.append("    </div>\n");

        // Checks
        html.append("    <div class=\"checks\">\n");
        for (var check : report.getChecks()) {
            html.append("      <div class=\"check\">\n");
            html.append("        <span class=\"status ").append(check.status()).append("\">").append(check.status())
                    .append("</span>\n");
            html.append("        <div class=\"check-info\">\n");
            html.append("          <div class=\"check-name\">").append(check.name());
            if (!check.nis2Article().isEmpty()) {
                html.append("<span class=\"article\">").append(check.nis2Article()).append("</span>");
            }
            html.append("</div>\n");
            html.append("          <div class=\"check-desc\">").append(check.description()).append("</div>\n");
            html.append("        </div>\n");
            html.append("      </div>\n");
        }
        html.append("    </div>\n");

        html.append("    <div class=\"footer\">\n");
        html.append("      Generated by NIS2 Spring Shield | <a href=\"https://nis2shield.com\">nis2shield.com</a>\n");
        html.append("    </div>\n");
        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Write HTML report to file.
     */
    public void writeHtmlToFile(String filePath) throws IOException {
        String html = generateHtml();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(html);
        }
    }

    /**
     * Write JSON report to file.
     */
    public void writeJsonToFile(String filePath) throws IOException {
        String json = generateJson();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write(json);
        }
    }

    /**
     * Get the underlying compliance checker.
     */
    public Nis2ComplianceChecker getComplianceChecker() {
        return complianceChecker;
    }
}
