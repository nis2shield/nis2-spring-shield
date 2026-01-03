package com.nis2shield.spring.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Checks if an IP is a known Tor Exit Node.
 */
public class TorBlocker {

    private static final Logger logger = LoggerFactory.getLogger(TorBlocker.class);
    private final Set<String> blockedIps;

    public TorBlocker() {
        this.blockedIps = new HashSet<>();
        // In a real scenario, this would load from a file or external URL on startup
        // blockedIps.add("1.2.3.4");
    }

    public boolean isBlocked(String ip) {
        return blockedIps.contains(ip);
    }

    public void updateList(Set<String> newIps) {
        // Thread-safe replacement? Copy on write might be better but for now simple set
        // clear/add
        synchronized (blockedIps) {
            blockedIps.clear();
            blockedIps.addAll(newIps);
        }
        logger.info("Updated Tor Blocklist with {} IPs", blockedIps.size());
    }

    public int getBlockedCount() {
        return blockedIps.size();
    }
}
