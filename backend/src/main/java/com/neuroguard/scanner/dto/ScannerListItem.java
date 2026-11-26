package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.CalibrationResult;
import com.neuroguard.scanner.entity.QaStatus;
import com.neuroguard.scanner.entity.ScannerStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One row of the scanner list. riskScore (indicative) is that of the latest session, null if none. */
public record ScannerListItem(
        UUID id,
        String name,
        String manufacturer,
        String model,
        double fieldStrength,
        String stationName,
        ScannerStatus status,
        QaStatus qaStatus,
        Instant lastQaAt,
        CalibrationResult lastQaResult,
        LocalDate nextDueDate,
        Double riskScore) {
}
