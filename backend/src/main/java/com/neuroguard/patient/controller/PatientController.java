package com.neuroguard.patient.controller;

import com.neuroguard.common.PageResponse;
import com.neuroguard.patient.dto.PatientSummary;
import com.neuroguard.patient.service.PatientService;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping
    @PreAuthorize(Authz.PATIENT_LIST)
    public PageResponse<PatientSummary> list(@RequestParam(required = false) String q,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        return patientService.list(q, page, size);
    }

    @GetMapping("/by-number/{patientNumber}")
    @PreAuthorize(Authz.PATIENT_LIST)
    public PatientSummary getByNumber(@PathVariable String patientNumber) {
        return patientService.getByPatientNumber(patientNumber);
    }

    @GetMapping("/{patientId}")
    @PreAuthorize(Authz.PATIENT_PROFILE_READ)
    public PatientSummary get(@PathVariable UUID patientId) {
        return patientService.getSummary(patientId);
    }
}
