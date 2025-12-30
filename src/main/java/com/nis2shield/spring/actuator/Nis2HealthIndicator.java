package com.nis2shield.spring.actuator;

import com.nis2shield.spring.security.TorBlocker;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class Nis2HealthIndicator implements HealthIndicator {

    private final TorBlocker torBlocker;

    public Nis2HealthIndicator(TorBlocker torBlocker) {
        this.torBlocker = torBlocker;
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("nis2_shield", "Active")
                .withDetail("blocked_tor_ips", torBlocker.getBlockedCount())
                .build();
    }
}
