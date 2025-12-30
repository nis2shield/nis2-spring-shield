package com.nis2shield.spring.security;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void testRateLimitAllowsWithinCapacity() {
        RateLimiter limiter = new RateLimiter(10, Duration.ofMinutes(1));
        String ip = "192.168.1.1";
        
        assertTrue(limiter.tryConsume(ip), "First request should be allowed");
        assertTrue(limiter.tryConsume(ip), "Second request should be allowed");
    }

    @Test
    void testRateLimitBlocksExceededCapacity() {
        // Capacity 1, Window 1 second
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(1));
        String ip = "192.168.1.2";
        
        assertTrue(limiter.tryConsume(ip), "1st Allowed");
        assertFalse(limiter.tryConsume(ip), "2nd Blocked immediately");
    }
}
