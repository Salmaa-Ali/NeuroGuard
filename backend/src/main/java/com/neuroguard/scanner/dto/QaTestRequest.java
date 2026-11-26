package com.neuroguard.scanner.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Create or replace a QA test. Limits are optional (null = no bound on that side). {@code placeholder}
 * defaults to false and {@code active} to true when omitted. Deactivate with active=false (no delete).
 */
public record QaTestRequest(
        @NotBlank(message = "must not be blank") @Size(max = 50) String code,
        @NotBlank(message = "must not be blank") @Size(max = 255) String name,
        @Size(max = 30) String unit,
        @Digits(integer = 8, fraction = 4, message = "must have at most 8 digits and 4 decimals") BigDecimal lowerLimit,
        @Digits(integer = 8, fraction = 4, message = "must have at most 8 digits and 4 decimals") BigDecimal upperLimit,
        String reference,
        Boolean placeholder,
        Boolean active) {
}
