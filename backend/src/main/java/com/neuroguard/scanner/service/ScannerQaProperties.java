package com.neuroguard.scanner.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** app.scanner.qa.due-soon-days: how many days before the due date a scanner counts as DUE_SOON. */
@ConfigurationProperties(prefix = "app.scanner.qa")
public record ScannerQaProperties(int dueSoonDays) {

    public ScannerQaProperties {
        if (dueSoonDays < 0) {
            throw new IllegalArgumentException("app.scanner.qa.due-soon-days must not be negative");
        }
    }
}
