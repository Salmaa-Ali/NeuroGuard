package com.neuroguard.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * Test clock: follows the real time until a test fixes it with {@link #setInstant}. Reset before every
 * integration test by {@link AbstractIntegrationTest}, so tests of other modules see the real time.
 */
public class MutableClock extends Clock {

    private volatile Instant fixed;

    public void setInstant(Instant instant) {
        this.fixed = instant;
    }

    public void advance(Duration duration) {
        this.fixed = instant().plus(duration);
    }

    public void reset() {
        this.fixed = null;
    }

    @Override
    public Instant instant() {
        Instant f = fixed;
        return f != null ? f : Instant.now();
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
