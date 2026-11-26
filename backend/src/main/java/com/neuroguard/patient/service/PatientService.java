package com.neuroguard.patient.service;

import com.neuroguard.common.PageResponse;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.patient.dto.PatientSummary;
import com.neuroguard.patient.repository.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PatientService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<PatientSummary> list(String q, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        String query = q == null ? null : q.trim();
        Page<PatientSummary> result = patientRepository
                .search(query, PageRequest.of(safePage, safeSize))
                .map(PatientSummary::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public PatientSummary getSummary(UUID patientId) {
        return patientRepository.findById(patientId)
                .map(PatientSummary::from)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }

    @Transactional(readOnly = true)
    public PatientSummary getByPatientNumber(String patientNumber) {
        return patientRepository.findByPatientNumber(patientNumber.trim())
                .map(PatientSummary::from)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientNumber));
    }
}
