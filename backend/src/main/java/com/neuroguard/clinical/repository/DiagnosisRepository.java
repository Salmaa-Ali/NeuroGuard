package com.neuroguard.clinical.repository;

import com.neuroguard.clinical.entity.Diagnosis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    @EntityGraph(attributePaths = {"icd10", "staff"})
    Page<Diagnosis> findByPatientId(UUID patientId, Pageable pageable);

    @EntityGraph(attributePaths = {"icd10", "staff"})
    Optional<Diagnosis> findWithDetailsById(UUID id);
}
