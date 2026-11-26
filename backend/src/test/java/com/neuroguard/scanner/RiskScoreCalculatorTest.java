package com.neuroguard.scanner;

import com.neuroguard.scanner.service.RiskScoreCalculator;
import com.neuroguard.scanner.service.RiskScoreCalculator.Point;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskScoreCalculatorTest {

    private static Point p(String value, String lower, String upper, boolean within) {
        return new Point(new BigDecimal(value), lower == null ? null : new BigDecimal(lower),
                upper == null ? null : new BigDecimal(upper), within);
    }

    private static final Point CENTRE = p("190", "188", "192", true);
    private static final Point ON_LIMIT = p("192", "188", "192", true);
    private static final Point FAILED = p("195", "188", "192", false);
    private static final Point ONE_SIDED = p("2.4", null, "2.5", true);

    @Test
    void perfectSessionOnTimeScoresZero() {
        assertEquals(0.0, RiskScoreCalculator.score(List.of(List.of(CENTRE, CENTRE)), 0, 90));
    }

    @Test
    void emptyInputScoresZero() {
        assertEquals(0.0, RiskScoreCalculator.score(List.of(), 0, 90));
        assertEquals(0.0, RiskScoreCalculator.score(List.of(List.of()), 0, 90));
    }

    @Test
    void worstCaseIsCappedAt100() {
        // all failed (F=1), no qualifying drift (D=0), very late (O=1): 50 + 0 + 20 = 70
        assertEquals(70.0, RiskScoreCalculator.score(List.of(List.of(FAILED)), 10_000, 90));
        // F=1 is the most failures can add; passing-on-limit drift cannot co-exist with F=1, so max stays <= 100
        double s = RiskScoreCalculator.score(List.of(List.of(FAILED, ON_LIMIT)), 10_000, 1);
        assertTrue(s >= 0 && s <= 100, "score " + s);
    }

    @Test
    void increasesWithFailures() {
        double none = RiskScoreCalculator.score(List.of(List.of(CENTRE, CENTRE, CENTRE, CENTRE)), 0, 90);
        double one = RiskScoreCalculator.score(List.of(List.of(CENTRE, CENTRE, CENTRE, FAILED)), 0, 90);
        double two = RiskScoreCalculator.score(List.of(List.of(CENTRE, CENTRE, FAILED, FAILED)), 0, 90);
        assertTrue(none < one && one < two, none + " < " + one + " < " + two);
        assertEquals(12.5, one); // 100 * 0.5 * 1/4
    }

    @Test
    void increasesWithDaysOverdueAndCapsAtOneInterval() {
        List<List<Point>> s = List.of(List.of(CENTRE));
        double onTime = RiskScoreCalculator.score(s, 0, 90);
        double late = RiskScoreCalculator.score(s, 45, 90);
        double veryLate = RiskScoreCalculator.score(s, 90, 90);
        double extremely = RiskScoreCalculator.score(s, 900, 90);
        assertTrue(onTime < late && late < veryLate, onTime + " < " + late + " < " + veryLate);
        assertEquals(10.0, late);     // 100 * 0.2 * 0.5
        assertEquals(20.0, veryLate);
        assertEquals(veryLate, extremely);
    }

    @Test
    void driftGrowsAsPassingValuesApproachALimit() {
        assertEquals(0.0, RiskScoreCalculator.score(List.of(List.of(CENTRE)), 0, 90));
        assertEquals(15.0, RiskScoreCalculator.score(List.of(List.of(p("191", "188", "192", true))), 0, 90)); // halfway
        assertEquals(30.0, RiskScoreCalculator.score(List.of(List.of(ON_LIMIT)), 0, 90));
    }

    @Test
    void oneSidedAndUnboundedValuesDoNotCountForDrift() {
        assertEquals(0.0, RiskScoreCalculator.score(List.of(List.of(ONE_SIDED, p("7", null, null, true))), 0, 90));
    }

    @Test
    void looksAcrossAllSessionsGiven() {
        double withHistory = RiskScoreCalculator.score(List.of(List.of(CENTRE), List.of(FAILED), List.of(CENTRE)), 0, 90);
        assertEquals(16.7, withHistory); // 100 * 0.5 * 1/3, rounded to 1 decimal
    }

    @Test
    void alwaysWithinZeroTo100() {
        for (int late : new int[]{0, 1, 30, 365, 100_000}) {
            for (List<Point> session : List.of(List.of(CENTRE), List.of(FAILED), List.of(ON_LIMIT, FAILED), List.of(ONE_SIDED))) {
                double s = RiskScoreCalculator.score(List.of(session, session, session), late, 30);
                assertTrue(s >= 0 && s <= 100, "score " + s);
            }
        }
    }
}
