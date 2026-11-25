package com.neuroguard.billing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Money rules for billing: BigDecimal, scale 2, HALF_UP, and the NUMERIC(10,2) ceiling. */
public final class Money {

    /** Largest amount a NUMERIC(10,2) column can store. */
    public static final BigDecimal MAX = new BigDecimal("99999999.99");

    private Money() {
    }

    public static BigDecimal of(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean exceedsMax(BigDecimal amount) {
        return amount.compareTo(MAX) > 0;
    }
}
