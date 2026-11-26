package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.CalibrationResult;
import com.neuroguard.scanner.entity.CalibrationSession;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** A session without its measurements. riskScore is indicative only. */
public record CalibrationSummary(
        UUID id,
        UUID scannerId,
        Instant performedAt,
        UUID performedById,
        String performedByName,
        CalibrationResult result,
        LocalDate nextDueDate,
        double riskScore,
        String certificateRef,
        Instant recordedAt) {

    public static CalibrationSummary from(CalibrationSession s) {
        return new CalibrationSummary(s.getId(), s.getScanner().getId(), s.getPerformedAt(),
                s.getPerformedBy().getId(), s.getPerformedBy().getName(), s.getResult(), s.getNextDueDate(),
                s.getRiskScore(), s.getCertificateRef(), s.getCreatedAt());
    }
}
