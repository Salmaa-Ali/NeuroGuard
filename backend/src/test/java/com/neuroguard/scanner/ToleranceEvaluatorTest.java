package com.neuroguard.scanner;

import com.neuroguard.scanner.service.ToleranceEvaluator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** PASS/FAIL per measurement: inclusive bounds, null = no bound on that side. */
class ToleranceEvaluatorTest {

    private static boolean within(String value, String lower, String upper) {
        return ToleranceEvaluator.isWithin(new BigDecimal(value),
                lower == null ? null : new BigDecimal(lower), upper == null ? null : new BigDecimal(upper));
    }

    @Test
    void twoSidedLimitsIncludeBothBoundaries() {
        assertTrue(within("188", "188", "192"));
        assertTrue(within("192", "188", "192"));
        assertTrue(within("190", "188", "192"));
        assertFalse(within("187.9999", "188", "192"));
        assertFalse(within("192.0001", "188", "192"));
    }

    @Test
    void lowerOnlyLimit() {
        assertTrue(within("87.5", "87.5", null));
        assertTrue(within("1000", "87.5", null));
        assertFalse(within("87.4", "87.5", null));
    }

    @Test
    void upperOnlyLimit() {
        assertTrue(within("2.5", null, "2.5"));
        assertTrue(within("-1000", null, "2.5"));
        assertFalse(within("2.51", null, "2.5"));
    }

    @Test
    void noLimitsAlwaysPass() {
        assertTrue(within("123456", null, null));
        assertTrue(within("-5", null, null));
    }

    @Test
    void scaleDoesNotMatter() {
        assertTrue(within("5.0000", "4.3", "5.70"));
        assertTrue(within("5.7", "4.3", "5.7000"));
    }
}
