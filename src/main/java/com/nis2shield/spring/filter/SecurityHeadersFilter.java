package com.nis2shield.spring.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import com.nis2shield.spring.configuration.Nis2Properties;

import java.io.IOException;

/**
 * SecurityHeadersFilter automatically adds security-related HTTP headers 
 * to all responses, improving the security posture of the application.
 * 
 * <p>This filter sets the following headers (when enabled):</p>
 * <ul>
 *   <li><b>Strict-Transport-Security (HSTS)</b>: Forces HTTPS for 1 year</li>
 *   <li><b>X-Content-Type-Options</b>: Prevents MIME-type sniffing</li>
 *   <li><b>X-Frame-Options</b>: Prevents clickjacking attacks</li>
 *   <li><b>Content-Security-Policy</b>: Basic CSP for XSS protection</li>
 *   <li><b>Referrer-Policy</b>: Controls referrer information</li>
 *   <li><b>Permissions-Policy</b>: Restricts browser features</li>
 * </ul>
 * 
 * <p>NIS2 Art. 21 requires appropriate security measures - security headers
 * are a fundamental part of web application security.</p>
 */
public class SecurityHeadersFilter implements Filter {

    private final Nis2Properties properties;

    public SecurityHeadersFilter(Nis2Properties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse httpResponse) {
            Nis2Properties.SecurityHeaders config = properties.getSecurityHeaders();

            if (config.isHstsEnabled()) {
                // HSTS: max-age=31536000 (1 year), include subdomains
                httpResponse.setHeader("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload");
            }

            if (config.isXContentTypeOptionsEnabled()) {
                httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            }

            if (config.isXFrameOptionsEnabled()) {
                httpResponse.setHeader("X-Frame-Options", "DENY");
            }

            if (config.isCspEnabled()) {
                // Basic CSP - applications should customize this
                httpResponse.setHeader("Content-Security-Policy", 
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self';");
            }

            if (config.isReferrerPolicyEnabled()) {
                httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            }

            if (config.isPermissionsPolicyEnabled()) {
                httpResponse.setHeader("Permissions-Policy", 
                    "geolocation=(), microphone=(), camera=()");
            }
        }

        chain.doFilter(request, response);
    }
}
