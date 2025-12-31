package com.nis2shield.spring.compliance;

import com.nis2shield.spring.configuration.Nis2Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComplianceReportService.
 */
class ComplianceReportServiceTest {

    private ComplianceReportService reportService;
    private Nis2Properties properties;

    @BeforeEach
    void setUp() {
        properties = new Nis2Properties();
        properties.setEnabled(true);
        properties.setIntegrityKey("test-key-that-is-long-enough-32-chars");
        properties.setEncryptionKey("16-char-key-aes!");
        
        Nis2ComplianceChecker checker = new Nis2ComplianceChecker(properties);
        reportService = new ComplianceReportService(checker);
    }

    @Test
    @DisplayName("Should generate valid JSON report")
    void shouldGenerateValidJson() {
        String json = reportService.generateJson();
        
        assertNotNull(json);
        assertTrue(json.contains("\"generatedAt\""));
        assertTrue(json.contains("\"score\""));
        assertTrue(json.contains("\"checks\""));
        assertTrue(json.contains("\"NIS2 Shield Enabled\""));
    }

    @Test
    @DisplayName("Should generate valid HTML report")
    void shouldGenerateValidHtml() {
        String html = reportService.generateHtml();
        
        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("NIS2 Compliance Report"));
        assertTrue(html.contains("nis2shield.com"));
        assertTrue(html.contains("PASS") || html.contains("FAIL") || html.contains("WARN"));
    }

    @Test
    @DisplayName("Should write HTML to file")
    void shouldWriteHtmlToFile(@TempDir Path tempDir) throws IOException {
        Path reportPath = tempDir.resolve("report.html");
        
        reportService.writeHtmlToFile(reportPath.toString());
        
        assertTrue(Files.exists(reportPath));
        String content = Files.readString(reportPath);
        assertTrue(content.contains("<!DOCTYPE html>"));
    }

    @Test
    @DisplayName("Should write JSON to file")
    void shouldWriteJsonToFile(@TempDir Path tempDir) throws IOException {
        Path reportPath = tempDir.resolve("report.json");
        
        reportService.writeJsonToFile(reportPath.toString());
        
        assertTrue(Files.exists(reportPath));
        String content = Files.readString(reportPath);
        assertTrue(content.contains("\"score\""));
    }

    @Test
    @DisplayName("HTML should have correct styling classes")
    void htmlShouldHaveCorrectStylingClasses() {
        String html = reportService.generateHtml();
        
        // Check for styling classes
        assertTrue(html.contains("class=\"score"));
        assertTrue(html.contains("class=\"check\""));
        assertTrue(html.contains("class=\"status"));
    }
}
