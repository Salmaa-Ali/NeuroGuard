package com.neuroguard.clinical.repository;

import com.neuroguard.clinical.entity.TreatmentPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, UUID> {

    @EntityGraph(attributePaths = "staff")
    Page<TreatmentPlan> findByPatientId(UUID patientId, Pageable pageable);
}
