package com.neuroguard.patient.service;

import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.patient.dto.PatientSummary;
import com.neuroguard.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public PatientSummary getSummary(UUID patientId) {
        return patientRepository.findById(patientId)
                .map(PatientSummary::from)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
    }
}
