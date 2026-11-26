package com.neuroguard.scanner.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

/**
 * Indicative QA risk score from 0 to 100. It helps the biomedical engineer prioritise reviews; it is
 * <b>not</b> a safety verdict and never blocks or changes any patient study or report.
 *
 * <pre>
 * risk = round1( 100 * (0.5 * F + 0.3 * D + 0.2 * O) ), clamped to [0, 100]
 *
 * F (failures) = failed measurements / all measurements, over the last 3 sessions (including the new one)
 * D (drift)    = average "closeness to a limit" of the PASSING measurements that have both limits, over the
 *                same sessions. For one value: margin = min(value - lower, upper - value);
 *                closeness = 1 - margin / ((upper - lower) / 2), so 0 = centre of the band, 1 = on a limit.
 *                Measurements with a one-sided or no tolerance (or lower == upper) are left out. 0 if none qualify.
 * O (overdue)  = days the new session was performed after the previous session's next_due_date,
 *                divided by the scanner's calibration interval, capped at 1. 0 for the first session.
 * </pre>
 *
 * The weights are a documented heuristic, not a standard. The score is stored on the session when it is
 * recorded, so it reflects the situation at that moment.
 */
public final class RiskScoreCalculator {

    public static final double WEIGHT_FAILURES = 0.5;
    public static final double WEIGHT_DRIFT = 0.3;
    public static final double WEIGHT_OVERDUE = 0.2;

    /** One measurement as used by the score (the limits are the ones copied onto the measurement). */
    public record Point(BigDecimal value, BigDecimal lower, BigDecimal upper, boolean withinTolerance) {
    }

    private RiskScoreCalculator() {
    }

    /**
     * @param recentSessions measurements of up to the last 3 sessions (the new one included)
     * @param daysOverdue    days the new session was late (0 if on time or first session)
     * @param intervalDays   the scanner's calibration interval (> 0)
     */
    public static double score(List<List<Point>> recentSessions, long daysOverdue, int intervalDays) {
        List<Point> points = recentSessions.stream().flatMap(List::stream).toList();
        double raw = 100 * (WEIGHT_FAILURES * failureShare(points)
                + WEIGHT_DRIFT * drift(points)
                + WEIGHT_OVERDUE * overdue(daysOverdue, intervalDays));
        double clamped = Math.max(0, Math.min(100, raw));
        return Math.round(clamped * 10) / 10.0;
    }

    static double failureShare(List<Point> points) {
        if (points.isEmpty()) {
            return 0;
        }
        long failed = points.stream().filter(p -> !p.withinTolerance()).count();
        return (double) failed / points.size();
    }

    static double drift(List<Point> points) {
        double sum = 0;
        int counted = 0;
        for (Point p : points) {
            if (!p.withinTolerance() || p.lower() == null || p.upper() == null) {
                continue;
            }
            BigDecimal halfRange = p.upper().subtract(p.lower()).divide(BigDecimal.valueOf(2), MathContext.DECIMAL64);
            if (halfRange.signum() <= 0) {
                continue;
            }
            BigDecimal margin = p.value().subtract(p.lower()).min(p.upper().subtract(p.value()));
            double closeness = 1 - margin.divide(halfRange, MathContext.DECIMAL64).doubleValue();
            sum += Math.max(0, Math.min(1, closeness));
            counted++;
        }
        return counted == 0 ? 0 : sum / counted;
    }

    static double overdue(long daysOverdue, int intervalDays) {
        if (daysOverdue <= 0 || intervalDays <= 0) {
            return 0;
        }
        return Math.min(1, (double) daysOverdue / intervalDays);
    }
}
