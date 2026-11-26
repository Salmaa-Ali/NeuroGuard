package com.neuroguard.scanner.service;

import java.math.BigDecimal;

/** Tolerance check for one measured value. */
public final class ToleranceEvaluator {

    private ToleranceEvaluator() {
    }

    /**
     * True if {@code lower <= value <= upper}. Both bounds are inclusive; a null bound means "no limit on
     * that side", so a test with no limits at all always passes.
     */
    public static boolean isWithin(BigDecimal value, BigDecimal lower, BigDecimal upper) {
        if (lower != null && value.compareTo(lower) < 0) {
            return false;
        }
        return upper == null || value.compareTo(upper) <= 0;
    }
}
