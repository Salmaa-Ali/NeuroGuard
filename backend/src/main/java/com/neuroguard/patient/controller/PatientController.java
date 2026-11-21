package com.neuroguard.patient.controller;

import com.neuroguard.patient.dto.PatientSummary;
import com.neuroguard.patient.service.PatientService;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    /** Minimal read-only endpoint; the full patient module comes later. */
    @GetMapping("/{patientId}")
    @PreAuthorize(Authz.PATIENT_PROFILE_READ)
    public PatientSummary get(@PathVariable UUID patientId) {
        return patientService.getSummary(patientId);
    }
}
