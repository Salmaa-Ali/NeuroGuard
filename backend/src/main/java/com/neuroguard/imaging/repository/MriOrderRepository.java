package com.neuroguard.imaging.repository;

import com.neuroguard.imaging.entity.MriOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MriOrderRepository extends JpaRepository<MriOrder, UUID> {

    Optional<MriOrder> findByAccessionNumber(String accessionNumber);
    Page<MriOrder> findAllBy(Pageable pageable);
    Page<MriOrder> findByPatientId(UUID patientId, Pageable pageable);
}
