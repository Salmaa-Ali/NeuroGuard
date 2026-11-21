package com.neuroguard.billing.repository;

import com.neuroguard.billing.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {
}
