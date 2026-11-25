package com.neuroguard.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** endDate is optional (open-ended plan); if given it must be on or after startDate. */
public record CreateTreatmentPlanRequest(
        @NotBlank(message = "must not be blank") @Size(max = 5000, message = "must be at most 5000 characters") String description,
        @NotNull(message = "is required") LocalDate startDate,
        LocalDate endDate) {
}
