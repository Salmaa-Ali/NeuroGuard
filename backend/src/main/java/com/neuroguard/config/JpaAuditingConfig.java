package com.neuroguard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Kept separate from the main class so web-layer test slices do not require JPA. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
