package com.neuroguard.clinical.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Free text, e.g. "Levetiracetam 500 mg, twice daily, oral". */
public record CreatePrescriptionRequest(
        @NotBlank(message = "must not be blank") @Size(max = 2000, message = "must be at most 2000 characters")
        String medicationDetails) {
}
