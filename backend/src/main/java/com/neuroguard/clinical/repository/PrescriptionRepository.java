package com.neuroguard.clinical.repository;

import com.neuroguard.clinical.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PrescriptionRepository extends JpaRepository<Prescription, UUID> {

    List<Prescription> findByPlanIdOrderByCreatedAtAscIdAsc(UUID planId);

    /** All prescriptions of a page of plans in one query (avoids one query per plan). */
    List<Prescription> findByPlanIdInOrderByCreatedAtAscIdAsc(Collection<UUID> planIds);
}
