package com.neuroguard.billing.repository;

import com.neuroguard.billing.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Override
    @EntityGraph(attributePaths = "invoice")
    Page<Payment> findAll(Pageable pageable);

    List<Payment> findByInvoiceIdIn(Collection<UUID> invoiceIds);
}
