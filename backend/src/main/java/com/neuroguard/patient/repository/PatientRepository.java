package com.neuroguard.patient.repository;

import com.neuroguard.patient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PatientRepository extends JpaRepository<Patient, UUID> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPatientNumber(String patientNumber);

    boolean existsByEmail(String email);

    boolean existsByPatientNumber(String patientNumber);

    @Query(value = "SELECT nextval('patient_number_seq')", nativeQuery = true)
    Long nextPatientNumberSeq();
}
