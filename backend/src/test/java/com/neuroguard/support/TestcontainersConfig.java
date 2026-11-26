package com.neuroguard.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * One Postgres container per Spring context (and the context is cached across test classes),
 * so Flyway runs for real. {@code @ServiceConnection} overrides any DB_URL from backend/.env,
 * so tests can never touch the shared Neon database.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16");
    }

    /** Replaces the app's system clock; follows real time unless a test fixes it. */
    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }
}
