package com.nis2shield.spring.configuration;

import com.nis2shield.spring.filter.Nis2AuditingFilter;
import com.nis2shield.spring.filter.SecurityHeadersFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import com.nis2shield.spring.filter.ActiveDefenseFilter;
import com.nis2shield.spring.security.RateLimiter;
import com.nis2shield.spring.security.TorBlocker;
import com.nis2shield.spring.utils.KeyRotationManager;
import com.nis2shield.spring.compliance.Nis2ComplianceChecker;
import com.nis2shield.spring.compliance.Nis2ComplianceRunner;
import com.nis2shield.spring.compliance.ComplianceReportService;
import java.time.Duration;
import org.springframework.boot.actuate.health.HealthIndicator;

@AutoConfiguration
@ConditionalOnWebApplication
@EnableConfigurationProperties(Nis2Properties.class)
@ConditionalOnProperty(prefix = "nis2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Nis2ShieldAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "nis2.logging", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<Nis2AuditingFilter> nis2AuditingFilter(
            Nis2Properties properties, 
            ObjectMapper objectMapper,
            com.nis2shield.spring.utils.CryptoUtils cryptoUtils) {
            
        FilterRegistrationBean<Nis2AuditingFilter> registrationBean = new FilterRegistrationBean<>();
        
        Nis2AuditingFilter filter = new Nis2AuditingFilter(properties, objectMapper, cryptoUtils);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(-100); // High priority (run early)
        registrationBean.addUrlPatterns("/*");
        
        return registrationBean;
    }
    
    @Bean
    public RateLimiter rateLimiter(Nis2Properties properties) {
        return new RateLimiter(
            properties.getActiveDefense().getRateLimitCapacity(),
            Duration.ofSeconds(properties.getActiveDefense().getRateLimitWindowSeconds())
        );
    }
    
    @Bean
    public TorBlocker torBlocker() {
        return new TorBlocker();
    }
    
    @Bean
    public com.nis2shield.spring.utils.CryptoUtils cryptoUtils(Nis2Properties properties) {
        return new com.nis2shield.spring.utils.CryptoUtils(properties.getEncryptionKey());
    }
    
    @Bean
    public KeyRotationManager keyRotationManager() {
        return new KeyRotationManager(90); // 90-day rotation interval
    }
    
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnClass(HealthIndicator.class)
    public com.nis2shield.spring.actuator.Nis2HealthIndicator nis2HealthIndicator(TorBlocker torBlocker) {
        return new com.nis2shield.spring.actuator.Nis2HealthIndicator(torBlocker);
    }
    
    @Bean
    public FilterRegistrationBean<ActiveDefenseFilter> activeDefenseFilter(
            Nis2Properties properties, 
            RateLimiter rateLimiter, 
            TorBlocker torBlocker) {
            
        FilterRegistrationBean<ActiveDefenseFilter> registrationBean = new FilterRegistrationBean<>();
        
        ActiveDefenseFilter filter = new ActiveDefenseFilter(properties, rateLimiter, torBlocker);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(-90); 
        registrationBean.addUrlPatterns("/*");
        
        return registrationBean;
    }
    
    @Bean
    @ConditionalOnProperty(prefix = "nis2.security-headers", name = "enabled", havingValue = "true", matchIfMissing = true)
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter(Nis2Properties properties) {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        
        SecurityHeadersFilter filter = new SecurityHeadersFilter(properties);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(-80); // Run after Active Defense
        registrationBean.addUrlPatterns("/*");
        
        return registrationBean;
    }
    
    // =============== Compliance Engine Beans ===============
    
    @Bean
    public Nis2ComplianceChecker nis2ComplianceChecker(Nis2Properties properties) {
        return new Nis2ComplianceChecker(properties);
    }
    
    @Bean
    public Nis2ComplianceRunner nis2ComplianceRunner(Nis2ComplianceChecker complianceChecker) {
        return new Nis2ComplianceRunner(complianceChecker);
    }
    
    @Bean
    public ComplianceReportService complianceReportService(Nis2ComplianceChecker complianceChecker) {
        return new ComplianceReportService(complianceChecker);
    }
}

