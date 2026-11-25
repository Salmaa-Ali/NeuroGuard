package com.neuroguard.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Deliberately has no price or total fields: the server copies unit prices from the catalogue and computes
 * every amount. Any price fields a client sends are ignored.
 */
public record CreateInvoiceRequest(
        @NotNull(message = "is required") UUID patientId,
        UUID appointmentId,
        @NotEmpty(message = "must contain at least one item")
        @Size(max = 100, message = "must contain at most 100 items")
        List<@Valid @NotNull(message = "must not be null") Item> items) {

    /**
     * {@code quantity} is capped as a sanity limit only; the real guard against overflow is the check that
     * every line total and the invoice total fit NUMERIC(10,2).
     */
    public record Item(
            @NotNull(message = "is required") UUID serviceId,
            @NotNull(message = "is required") @Min(value = 1, message = "must be at least 1")
            @Max(value = 1000, message = "must be at most 1000") Integer quantity,
            @Size(max = 10, message = "must be at most 10 characters") String icd10Code) {
    }
}
