package com.neuroguard.scanner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The client sends only what was measured; result, next due date and risk score are computed by the server. */
public record RecordCalibrationRequest(
        @NotNull(message = "is required") Instant performedAt,
        String notes,
        @Size(max = 100) String certificateRef,
        @NotEmpty(message = "must contain at least one measurement") List<@Valid Measurement> measurements) {

    public record Measurement(
            @NotNull(message = "is required") UUID qaTestId,
            @NotNull(message = "is required")
            @Digits(integer = 8, fraction = 4, message = "must have at most 8 digits and 4 decimals")
            BigDecimal measuredValue) {
    }
}
