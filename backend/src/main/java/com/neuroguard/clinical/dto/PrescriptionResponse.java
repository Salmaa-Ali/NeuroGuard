package com.neuroguard.clinical.dto;

import com.neuroguard.clinical.entity.Prescription;

import java.time.Instant;
import java.util.UUID;

/** {@code staffId}/{@code staffName} = the prescribing neurologist. */
public record PrescriptionResponse(UUID id, UUID planId, String medicationDetails, UUID staffId, String staffName,
                                   Instant createdAt) {

    public static PrescriptionResponse from(Prescription p) {
        return new PrescriptionResponse(p.getId(), p.getPlan().getId(), p.getMedicationDetails(),
                p.getStaff().getId(), p.getStaff().getName(), p.getCreatedAt());
    }
}
