package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.CalibrationResult;
import com.neuroguard.scanner.entity.CalibrationSession;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A session with all its measurements. riskScore is indicative only. */
public record CalibrationDetail(
        UUID id,
        UUID scannerId,
        String scannerName,
        Instant performedAt,
        UUID performedById,
        String performedByName,
        CalibrationResult result,
        LocalDate nextDueDate,
        double riskScore,
        String notes,
        String certificateRef,
        Instant recordedAt,
        List<MeasurementResponse> measurements) {

    public static CalibrationDetail from(CalibrationSession s, List<MeasurementResponse> measurements) {
        return new CalibrationDetail(s.getId(), s.getScanner().getId(), s.getScanner().getName(),
                s.getPerformedAt(), s.getPerformedBy().getId(), s.getPerformedBy().getName(), s.getResult(),
                s.getNextDueDate(), s.getRiskScore(), s.getNotes(), s.getCertificateRef(), s.getCreatedAt(),
                measurements);
    }
}
