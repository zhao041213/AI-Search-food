package com.example.food.config;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Exposes migration state as part of the container health check without leaking details. */
@Component("flywayMigration")
public class FlywayHealthIndicator implements HealthIndicator {

    private final Flyway flyway;

    public FlywayHealthIndicator(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public Health health() {
        try {
            MigrationInfo current = flyway.info().current();
            if (current == null) {
                return Health.down().withDetail("reason", "No Flyway migration has been applied").build();
            }
            return Health.up().withDetail("version", current.getVersion().getVersion()).build();
        } catch (RuntimeException exception) {
            return Health.down().withDetail("reason", "Flyway status unavailable").build();
        }
    }
}
