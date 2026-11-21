package com.neuroguard.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expiryMinutes, String issuer) {

    public JwtProperties {
        if (expiryMinutes <= 0) {
            throw new IllegalArgumentException("app.jwt.expiry-minutes must be positive");
        }
    }
}
