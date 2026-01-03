package com.nis2shield.spring.filter;

import com.nis2shield.spring.configuration.Nis2Properties;
import com.nis2shield.spring.security.RateLimiter;
import com.nis2shield.spring.security.TorBlocker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class ActiveDefenseFilter extends OncePerRequestFilter {

    private static final Logger defenseLogger = LoggerFactory.getLogger("NIS2_ACTIVE_DEFENSE");
    private final Nis2Properties properties;
    private final RateLimiter rateLimiter;
    private final TorBlocker torBlocker;

    public ActiveDefenseFilter(Nis2Properties properties, RateLimiter rateLimiter, TorBlocker torBlocker) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.torBlocker = torBlocker;
    }

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
            @org.springframework.lang.NonNull HttpServletResponse response,
            @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String ip = request.getRemoteAddr();

        // 1. Tor Blocking
        if (properties.getActiveDefense().isBlockTorExitNodes() && torBlocker.isBlocked(ip)) {
            defenseLogger.warn("NIS2 Blocked Tor Node: {}", ip);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied (High Risk IP)");
            return;
        }

        // 2. Rate Limiting
        if (properties.getActiveDefense().isRateLimitEnabled()) {
            if (!rateLimiter.tryConsume(ip)) {
                defenseLogger.warn("NIS2 Rate Limit Exceeded: {}", ip);
                response.addHeader("X-Rate-Limit-Retry-After-Seconds", "60");
                response.sendError(429, "Too Many Requests");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
