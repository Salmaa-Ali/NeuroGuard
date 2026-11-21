package com.neuroguard.clinical.repository;

import com.neuroguard.clinical.entity.Diagnosis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {
}
