package com.neuroguard.appointment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record CreateAppointmentRequest(@NotNull UUID patientId, UUID staffId,
        @NotNull Instant scheduledTime, @Min(5) @Max(480) int durationMinutes,
        @Size(max = 4000) String reason) { }
