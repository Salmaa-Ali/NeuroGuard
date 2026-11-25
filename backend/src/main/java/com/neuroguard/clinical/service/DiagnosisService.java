package com.neuroguard.clinical.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.clinical.dto.CreateDiagnosisRequest;
import com.neuroguard.clinical.dto.DiagnosisResponse;
import com.neuroguard.clinical.entity.Diagnosis;
import com.neuroguard.clinical.repository.DiagnosisRepository;
import com.neuroguard.common.BadRequestException;
import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.common.PatientRecordAccess;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.icd10.entity.Icd10Code;
import com.neuroguard.icd10.service.Icd10Service;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.CurrentUser;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class DiagnosisService {

    /** Newest first; createdAt and id break ties so paging is stable. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("diagnosedAt"), Sort.Order.desc("createdAt"),
            Sort.Order.asc("id"));

    private final DiagnosisRepository diagnosisRepository;
    private final PatientRepository patientRepository;
    private final StaffRepository staffRepository;
    private final Icd10Service icd10Service;
    private final AuditService auditService;

    public DiagnosisService(DiagnosisRepository diagnosisRepository, PatientRepository patientRepository,
                            StaffRepository staffRepository, Icd10Service icd10Service, AuditService auditService) {
        this.diagnosisRepository = diagnosisRepository;
        this.patientRepository = patientRepository;
        this.staffRepository = staffRepository;
        this.icd10Service = icd10Service;
        this.auditService = auditService;
    }

    /**
     * The ICD-10 code is checked here (400 with the field name) rather than left to the foreign key, which
     * would surface as a generic 409. The author is always the caller, so a diagnosis cannot be attributed
     * to another clinician. The audit row shares the transaction: no diagnosis without its audit trail.
     */
    @Transactional
    public DiagnosisResponse create(UUID patientId, CreateDiagnosisRequest request) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        Icd10Code code = icd10Service.find(request.icd10Code())
                .orElseThrow(() -> new BadRequestException("Unknown ICD-10 code: " + request.icd10Code(), "icd10Code"));
        CurrentUser actor = CurrentUser.require();
        Staff author = staffRepository.findById(actor.id())
                .orElseThrow(() -> new AccessDeniedException("Staff account no longer exists"));

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setPatient(patient);
        diagnosis.setStaff(author);
        diagnosis.setIcd10Code(code.getCode());
        diagnosis.setIcd10(code);
        diagnosis.setDiagnosedAt((request.diagnosedAt() == null ? Instant.now() : request.diagnosedAt())
                .truncatedTo(ChronoUnit.MICROS));
        diagnosis.setNotes(blankToNull(request.notes()));
        diagnosis = diagnosisRepository.saveAndFlush(diagnosis);

        auditService.record(actor.actorType(), actor.id(), AuditAction.DIAGNOSIS_CREATED, "DIAGNOSIS", diagnosis.getId());
        return DiagnosisResponse.from(diagnosis);
    }

    /** 404 for an unknown patient so staff can tell "no diagnoses yet" apart from a wrong id. */
    @Transactional(readOnly = true)
    public PageResponse<DiagnosisResponse> listForPatient(UUID patientId, int page, int size) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        return PageResponse.of(diagnosisRepository.findByPatientId(patientId, Pages.of(page, size, NEWEST_FIRST)),
                DiagnosisResponse::from);
    }

    /** Loaded by diagnosis id, so the "own" rule is enforced here, not in @PreAuthorize. */
    @Transactional(readOnly = true)
    public DiagnosisResponse get(UUID id) {
        Diagnosis diagnosis = PatientRecordAccess.requireReadable(diagnosisRepository.findWithDetailsById(id),
                d -> d.getPatient().getId(), "Diagnosis", id);
        return DiagnosisResponse.from(diagnosis);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
