package com.neuroguard.clinical.dto;

import com.neuroguard.clinical.entity.Diagnosis;

import java.time.Instant;
import java.util.UUID;

public record DiagnosisResponse(
        UUID id,
        UUID patientId,
        String icd10Code,
        String icd10Description,
        String icd10Category,
        Instant diagnosedAt,
        String notes,
        UUID staffId,
        String staffName,
        Instant createdAt) {

    public static DiagnosisResponse from(Diagnosis d) {
        return new DiagnosisResponse(d.getId(), d.getPatient().getId(), d.getIcd10Code(),
                d.getIcd10().getDescription(), d.getIcd10().getCategory(), d.getDiagnosedAt(), d.getNotes(),
                d.getStaff().getId(), d.getStaff().getName(), d.getCreatedAt());
    }
}
