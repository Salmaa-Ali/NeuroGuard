package com.neuroguard.scanner;

import com.neuroguard.scanner.entity.CalibrationResult;
import com.neuroguard.scanner.entity.QaStatus;
import com.neuroguard.scanner.service.QaStatusCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QaStatusCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
    private static final int N = 30;

    private static QaStatus status(CalibrationResult result, LocalDate due) {
        return QaStatusCalculator.status(result, due, TODAY, N);
    }

    @Test
    void neverCalibratedWithoutSession() {
        assertEquals(QaStatus.NEVER_CALIBRATED, status(null, null));
    }

    @Test
    void failedWinsOverDates() {
        assertEquals(QaStatus.FAILED, status(CalibrationResult.FAIL, TODAY.plusDays(300)));
        assertEquals(QaStatus.FAILED, status(CalibrationResult.FAIL, TODAY.minusDays(10)));
    }

    @Test
    void overdueOnlyAfterTheDueDate() {
        assertEquals(QaStatus.OVERDUE, status(CalibrationResult.PASS, TODAY.minusDays(1)));
        assertEquals(QaStatus.DUE_SOON, status(CalibrationResult.PASS, TODAY), "due today is not overdue yet");
    }

    @Test
    void dueSoonWithinNDaysInclusive() {
        assertEquals(QaStatus.DUE_SOON, status(CalibrationResult.PASS, TODAY.plusDays(N)));
        assertEquals(QaStatus.OK, status(CalibrationResult.PASS, TODAY.plusDays(N + 1)));
    }

    @Test
    void dueSoonWindowIsConfigurable() {
        assertEquals(QaStatus.OK, QaStatusCalculator.status(CalibrationResult.PASS, TODAY.plusDays(10), TODAY, 7));
        assertEquals(QaStatus.DUE_SOON, QaStatusCalculator.status(CalibrationResult.PASS, TODAY.plusDays(7), TODAY, 7));
    }

    @Test
    void daysOverdue() {
        assertEquals(0, QaStatusCalculator.daysOverdue(TODAY, TODAY));
        assertEquals(0, QaStatusCalculator.daysOverdue(TODAY.plusDays(5), TODAY));
        assertEquals(12, QaStatusCalculator.daysOverdue(TODAY.minusDays(12), TODAY));
    }
}
