package com.neuroguard.billing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Create or replace a catalogue entry. Price in EGP, 0 to 99,999,999.99 (fits NUMERIC(10,2)). */
public record ServiceRequest(
        @NotBlank(message = "must not be blank") @Size(max = 255, message = "must be at most 255 characters") String name,
        @NotNull(message = "is required")
        @DecimalMin(value = "0.00", message = "must be 0 or more")
        @Digits(integer = 8, fraction = 2, message = "must have at most 8 digits before and 2 after the decimal point")
        BigDecimal price) {
}
