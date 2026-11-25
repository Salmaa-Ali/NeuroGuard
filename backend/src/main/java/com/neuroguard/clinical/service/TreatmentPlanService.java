package com.neuroguard.clinical.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.clinical.dto.CreatePrescriptionRequest;
import com.neuroguard.clinical.dto.CreateTreatmentPlanRequest;
import com.neuroguard.clinical.dto.PrescriptionResponse;
import com.neuroguard.clinical.dto.TreatmentPlanResponse;
import com.neuroguard.clinical.entity.Prescription;
import com.neuroguard.clinical.entity.TreatmentPlan;
import com.neuroguard.clinical.repository.PrescriptionRepository;
import com.neuroguard.clinical.repository.TreatmentPlanRepository;
import com.neuroguard.common.BadRequestException;
import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.common.PatientRecordAccess;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.CurrentUser;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TreatmentPlanService {

    /** Most recently started plan first; createdAt and id keep paging stable. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("startDate"), Sort.Order.desc("createdAt"),
            Sort.Order.asc("id"));

    private final TreatmentPlanRepository planRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final PatientRepository patientRepository;
    private final StaffRepository staffRepository;
    private final AuditService auditService;

    public TreatmentPlanService(TreatmentPlanRepository planRepository, PrescriptionRepository prescriptionRepository,
                                PatientRepository patientRepository, StaffRepository staffRepository,
                                AuditService auditService) {
        this.planRepository = planRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.patientRepository = patientRepository;
        this.staffRepository = staffRepository;
        this.auditService = auditService;
    }

    /**
     * The date rule is checked here so the client gets a 400 naming the field; the database CHECK
     * (ck_treatment_plan_dates) is only the last line of defence and would surface as a generic 409.
     */
    @Transactional
    public TreatmentPlanResponse createPlan(UUID patientId, CreateTreatmentPlanRequest request) {
        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BadRequestException("endDate must be on or after startDate", "endDate");
        }
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        CurrentUser actor = CurrentUser.require();

        TreatmentPlan plan = new TreatmentPlan();
        plan.setPatient(patient);
        plan.setStaff(currentStaff(actor));
        plan.setDescription(request.description().trim());
        plan.setStartDate(request.startDate());
        plan.setEndDate(request.endDate());
        plan = planRepository.saveAndFlush(plan);

        auditService.record(actor.actorType(), actor.id(), AuditAction.TREATMENT_PLAN_CREATED, "TREATMENT_PLAN",
                plan.getId());
        return TreatmentPlanResponse.from(plan, List.of());
    }

    /**
     * Prescriptions are embedded because the patient's "My prescriptions" page and the clinician's view both
     * need plans and their medication together; they are fetched for the whole page in one query.
     */
    @Transactional(readOnly = true)
    public PageResponse<TreatmentPlanResponse> listForPatient(UUID patientId, int page, int size) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        Page<TreatmentPlan> plans = planRepository.findByPatientId(patientId, Pages.of(page, size, NEWEST_FIRST));
        List<UUID> planIds = plans.getContent().stream().map(TreatmentPlan::getId).toList();
        Map<UUID, List<PrescriptionResponse>> byPlan = planIds.isEmpty() ? Map.of()
                : prescriptionRepository.findByPlanIdInOrderByCreatedAtAscIdAsc(planIds).stream()
                .map(PrescriptionResponse::from)
                .collect(Collectors.groupingBy(PrescriptionResponse::planId));
        return PageResponse.of(plans, p -> TreatmentPlanResponse.from(p, byPlan.getOrDefault(p.getId(), List.of())));
    }

    /** Audited like other clinical writes; the audit row is the record of who prescribed. */
    @Transactional
    public PrescriptionResponse addPrescription(UUID planId, CreatePrescriptionRequest request) {
        TreatmentPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment plan", planId));
        CurrentUser actor = CurrentUser.require();

        Prescription prescription = new Prescription();
        prescription.setPlan(plan);
        prescription.setMedicationDetails(request.medicationDetails().trim());
        prescription = prescriptionRepository.saveAndFlush(prescription);

        auditService.record(actor.actorType(), actor.id(), AuditAction.PRESCRIPTION_CREATED, "PRESCRIPTION",
                prescription.getId());
        return PrescriptionResponse.from(prescription);
    }

    /** Loaded by plan id, so the "own" rule is checked against the plan's patient here. */
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> listPrescriptions(UUID planId) {
        TreatmentPlan plan = PatientRecordAccess.requireReadable(planRepository.findById(planId),
                p -> p.getPatient().getId(), "Treatment plan", planId);
        return prescriptionRepository.findByPlanIdOrderByCreatedAtAscIdAsc(plan.getId()).stream()
                .map(PrescriptionResponse::from).toList();
    }

    private Staff currentStaff(CurrentUser actor) {
        return staffRepository.findById(actor.id())
                .orElseThrow(() -> new AccessDeniedException("Staff account no longer exists"));
    }
}
