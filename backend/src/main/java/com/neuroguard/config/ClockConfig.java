package com.neuroguard.config;

import com.neuroguard.scanner.service.ScannerQaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Injectable clock (UTC) so date logic such as QA due dates can be tested with a fixed time. */
@Configuration
@EnableConfigurationProperties(ScannerQaProperties.class)
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
