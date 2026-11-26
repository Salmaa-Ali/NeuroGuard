package com.neuroguard.scanner.service;

import com.neuroguard.scanner.entity.CalibrationResult;
import com.neuroguard.scanner.entity.QaStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Derives a scanner's QA status from its latest session (by performed_at). Checked in this order:
 * <ol>
 *   <li>NEVER_CALIBRATED: no session at all</li>
 *   <li>FAILED: the latest session's result is FAIL</li>
 *   <li>OVERDUE: today is after the latest next_due_date</li>
 *   <li>DUE_SOON: the due date is today or within the next {@code dueSoonDays} days</li>
 *   <li>OK: otherwise</li>
 * </ol>
 * The status is a signal for review only: it never blocks, invalidates or changes patient studies or reports.
 */
public final class QaStatusCalculator {

    private QaStatusCalculator() {
    }

    /** @param latestResult null when the scanner has no session yet */
    public static QaStatus status(CalibrationResult latestResult, LocalDate nextDueDate, LocalDate today,
                                  int dueSoonDays) {
        if (latestResult == null) {
            return QaStatus.NEVER_CALIBRATED;
        }
        if (latestResult == CalibrationResult.FAIL) {
            return QaStatus.FAILED;
        }
        if (today.isAfter(nextDueDate)) {
            return QaStatus.OVERDUE;
        }
        long daysLeft = ChronoUnit.DAYS.between(today, nextDueDate);
        return daysLeft <= dueSoonDays ? QaStatus.DUE_SOON : QaStatus.OK;
    }

    /** Days past the due date, 0 if not overdue. */
    public static long daysOverdue(LocalDate nextDueDate, LocalDate day) {
        return Math.max(0, ChronoUnit.DAYS.between(nextDueDate, day));
    }
}
