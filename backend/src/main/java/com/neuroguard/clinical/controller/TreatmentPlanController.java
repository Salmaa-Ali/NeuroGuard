package com.neuroguard.clinical.controller;

import com.neuroguard.clinical.dto.CreatePrescriptionRequest;
import com.neuroguard.clinical.dto.CreateTreatmentPlanRequest;
import com.neuroguard.clinical.dto.PrescriptionResponse;
import com.neuroguard.clinical.dto.TreatmentPlanResponse;
import com.neuroguard.clinical.service.TreatmentPlanService;
import com.neuroguard.common.PageResponse;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Clinical - Treatment plans & prescriptions")
public class TreatmentPlanController {

    private final TreatmentPlanService planService;

    public TreatmentPlanController(TreatmentPlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/patients/{patientId}/treatment-plans")
    @PreAuthorize(Authz.CLINICAL_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentPlanResponse createPlan(@PathVariable UUID patientId,
                                            @Valid @RequestBody CreateTreatmentPlanRequest request) {
        return planService.createPlan(patientId, request);
    }

    /** Most recently started first; each plan includes its prescriptions. */
    @GetMapping("/patients/{patientId}/treatment-plans")
    @PreAuthorize(Authz.CLINICAL_READ_BY_PATIENT)
    public PageResponse<TreatmentPlanResponse> listPlans(@PathVariable UUID patientId,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) {
        return planService.listForPatient(patientId, page, size);
    }

    @PostMapping("/treatment-plans/{planId}/prescriptions")
    @PreAuthorize(Authz.CLINICAL_WRITE)
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse addPrescription(@PathVariable UUID planId,
                                                @Valid @RequestBody CreatePrescriptionRequest request) {
        return planService.addPrescription(planId, request);
    }

    /** Oldest first. */
    @GetMapping("/treatment-plans/{planId}/prescriptions")
    @PreAuthorize(Authz.CLINICAL_READ_BY_RECORD)
    public List<PrescriptionResponse> listPrescriptions(@PathVariable UUID planId) {
        return planService.listPrescriptions(planId);
    }
}
