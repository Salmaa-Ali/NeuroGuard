package com.neuroguard.clinical.dto;

import com.neuroguard.clinical.entity.TreatmentPlan;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** A plan with its prescriptions (oldest first), so the UI needs one call per patient. */
public record TreatmentPlanResponse(
        UUID id,
        UUID patientId,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        UUID staffId,
        String staffName,
        Instant createdAt,
        List<PrescriptionResponse> prescriptions) {

    public static TreatmentPlanResponse from(TreatmentPlan p, List<PrescriptionResponse> prescriptions) {
        return new TreatmentPlanResponse(p.getId(), p.getPatient().getId(), p.getDescription(), p.getStartDate(),
                p.getEndDate(), p.getStaff().getId(), p.getStaff().getName(), p.getCreatedAt(), prescriptions);
    }
}
