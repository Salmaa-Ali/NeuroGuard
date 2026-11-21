package com.neuroguard.patient.dto;

import com.neuroguard.patient.entity.Patient;

import java.time.LocalDate;
import java.util.UUID;

/** Read-only view of a patient. Never includes the password hash. */
public record PatientSummary(
        UUID id,
        String patientNumber,
        String name,
        String email,
        LocalDate dateOfBirth,
        String phone,
        String neurologyHistory) {

    public static PatientSummary from(Patient p) {
        return new PatientSummary(p.getId(), p.getPatientNumber(), p.getName(), p.getEmail(),
                p.getDateOfBirth(), p.getPhone(), p.getNeurologyHistory());
    }
}
