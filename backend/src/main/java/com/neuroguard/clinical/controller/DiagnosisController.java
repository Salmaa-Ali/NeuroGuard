package com.neuroguard.clinical.controller;

import com.neuroguard.clinical.dto.CreateDiagnosisRequest;
import com.neuroguard.clinical.dto.DiagnosisResponse;
import com.neuroguard.clinical.service.DiagnosisService;
import com.neuroguard.common.PageResponse;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Clinical - Diagnoses")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @PostMapping("/patients/{patientId}/diagnoses")
    @PreAuthorize(Authz.CLINICAL_WRITE)
    public ResponseEntity<DiagnosisResponse> create(@PathVariable UUID patientId,
                                                    @Valid @RequestBody CreateDiagnosisRequest request) {
        DiagnosisResponse created = diagnosisService.create(patientId, request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentContextPath()
                        .path("/api/diagnoses/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    /** Newest first. */
    @GetMapping("/patients/{patientId}/diagnoses")
    @PreAuthorize(Authz.CLINICAL_READ_BY_PATIENT)
    public PageResponse<DiagnosisResponse> list(@PathVariable UUID patientId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return diagnosisService.listForPatient(patientId, page, size);
    }

    @GetMapping("/diagnoses/{id}")
    @PreAuthorize(Authz.CLINICAL_READ_BY_RECORD)
    public DiagnosisResponse get(@PathVariable UUID id) {
        return diagnosisService.get(id);
    }
}
