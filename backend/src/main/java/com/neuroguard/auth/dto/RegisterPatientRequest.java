package com.neuroguard.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record RegisterPatientRequest(
        @NotBlank(message = "is required")
        @Size(max = 255, message = "must be at most 255 characters")
        String name,

        @NotBlank(message = "is required")
        @Email(message = "must be a valid email address")
        @Size(max = 255, message = "must be at most 255 characters")
        String email,

        @NotBlank(message = "is required")
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters")
        String password,

        @NotNull(message = "is required")
        @Past(message = "must be in the past")
        LocalDate dateOfBirth,

        @Size(max = 50, message = "must be at most 50 characters")
        String phone,

        @Size(max = 5000, message = "must be at most 5000 characters")
        String neurologyHistory
) {
}
