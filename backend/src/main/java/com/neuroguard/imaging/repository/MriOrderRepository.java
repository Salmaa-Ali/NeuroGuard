package com.neuroguard.imaging.repository;

import com.neuroguard.imaging.entity.MriOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MriOrderRepository extends JpaRepository<MriOrder, UUID> {

    Optional<MriOrder> findByAccessionNumber(String accessionNumber);
}
