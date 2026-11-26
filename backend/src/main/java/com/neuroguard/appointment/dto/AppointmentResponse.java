package com.neuroguard.appointment.dto;

import com.neuroguard.appointment.entity.Appointment;
import com.neuroguard.appointment.entity.AppointmentStatus;
import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(UUID id, UUID patientId, String patientName, UUID staffId,
        String staffName, Instant scheduledTime, int durationMinutes, AppointmentStatus status, String reason) {
    public static AppointmentResponse from(Appointment a) {
        return new AppointmentResponse(a.getId(), a.getPatient().getId(), a.getPatient().getName(),
                a.getStaff().getId(), a.getStaff().getName(), a.getScheduledTime(), a.getDurationMinutes(),
                a.getStatus(), a.getReason());
    }
}
