package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.CalibrationResult;
import com.neuroguard.scanner.entity.QaStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Compact, read-only QA status for the viewer's equipment panel. {@code flaggedForReview} is true for FAILED
 * and OVERDUE: a prompt to review only, it never affects any patient study or report.
 * Provenance: the values come from scanner {@code scannerId} and session {@code sourceSessionId}
 * (recorded at {@code sourceSessionRecordedAt}); the status was evaluated on {@code evaluatedOn}.
 */
public record QaSummary(
        UUID scannerId,
        String scannerName,
        double fieldStrength,
        QaStatus qaStatus,
        boolean flaggedForReview,
        Instant lastQaAt,
        CalibrationResult lastQaResult,
        LocalDate nextDueDate,
        Double riskScore,
        UUID sourceSessionId,
        Instant sourceSessionRecordedAt,
        LocalDate evaluatedOn) {
}
