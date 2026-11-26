package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.QaStatus;
import com.neuroguard.scanner.entity.ScannerStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScannerDetail(
        UUID id,
        String name,
        String manufacturer,
        String model,
        String serialNumber,
        String stationName,
        double fieldStrength,
        ScannerStatus status,
        String location,
        LocalDate installDate,
        int calibrationIntervalDays,
        QaStatus qaStatus,
        CalibrationSummary latestSession,
        List<AccessoryResponse> accessories,
        Instant createdAt,
        Instant updatedAt) {
}
