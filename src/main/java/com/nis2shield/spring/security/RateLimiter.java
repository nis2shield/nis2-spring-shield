package com.nis2shield.spring.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rate limiting buckets per IP address.
 */
public class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final long capacity;
    private final Duration window;

    public RateLimiter(long capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
    }

    public boolean tryConsume(String ip) {
        Bucket bucket = buckets.computeIfAbsent(ip, this::createNewBucket);
        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket(String key) {
        // Sliding window: Refill 1 token every (window / capacity) interval effectively, 
        // but Bucket4j 'greedy' refill is simpler for sliding window approximation or use true sliding window
        // Here we use classic bandwidth: Capacity tokens per Window.
        
        Refill refill = Refill.greedy(capacity, window);
        Bandwidth limit = Bandwidth.classic(capacity, refill);
        
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    public long getRemainingTokens(String ip) {
        Bucket bucket = buckets.get(ip);
        if (bucket == null) {
            return capacity;
        }
        return bucket.getAvailableTokens();
    }
}
