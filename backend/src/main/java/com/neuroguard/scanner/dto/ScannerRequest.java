package com.neuroguard.scanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Create or replace a scanner's details. Status has its own endpoint. */
public record ScannerRequest(
        @NotBlank(message = "must not be blank") @Size(max = 255) String name,
        @NotBlank(message = "must not be blank") @Size(max = 255) String manufacturer,
        @NotBlank(message = "must not be blank") @Size(max = 255) String model,
        @NotBlank(message = "must not be blank") @Size(max = 100) String serialNumber,
        @Size(max = 255) String stationName,
        @NotNull(message = "is required") @Positive(message = "must be greater than 0") Double fieldStrength,
        @Size(max = 255) String location,
        LocalDate installDate,
        @NotNull(message = "is required") @Positive(message = "must be greater than 0") Integer calibrationIntervalDays) {
}
