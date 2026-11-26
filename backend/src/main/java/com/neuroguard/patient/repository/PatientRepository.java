package com.neuroguard.patient.repository;

import com.neuroguard.patient.entity.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPatientNumber(String patientNumber);

    boolean existsByEmail(String email);

    boolean existsByPatientNumber(String patientNumber);

    @Query(value = "SELECT nextval('patient_number_seq')", nativeQuery = true)
    Long nextPatientNumberSeq();

    @Query("""
            SELECT p FROM Patient p
            WHERE (:q IS NULL OR :q = ''
                   OR LOWER(p.patientNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY p.patientNumber ASC
            """)
    Page<Patient> search(@Param("q") String q, Pageable pageable);
}
