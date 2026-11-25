package com.neuroguard.clinical.dto;

import com.neuroguard.clinical.entity.Prescription;

import java.time.Instant;
import java.util.UUID;

public record PrescriptionResponse(UUID id, UUID planId, String medicationDetails, Instant createdAt) {

    public static PrescriptionResponse from(Prescription p) {
        return new PrescriptionResponse(p.getId(), p.getPlan().getId(), p.getMedicationDetails(), p.getCreatedAt());
    }
}
