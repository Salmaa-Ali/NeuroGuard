package com.neuroguard.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * The patient comes from the URL and the diagnosing neurologist from the token, never from the body.
 * {@code diagnosedAt} defaults to now.
 */
public record CreateDiagnosisRequest(
        @NotBlank(message = "must not be blank") @Size(max = 10, message = "must be at most 10 characters") String icd10Code,
        @PastOrPresent(message = "must not be in the future") Instant diagnosedAt,
        @Size(max = 5000, message = "must be at most 5000 characters") String notes) {
}
