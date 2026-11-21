package com.neuroguard.clinical.repository;

import com.neuroguard.clinical.entity.TreatmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TreatmentPlanRepository extends JpaRepository<TreatmentPlan, UUID> {
}
