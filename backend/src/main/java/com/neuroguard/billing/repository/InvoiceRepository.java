package com.neuroguard.billing.repository;

import com.neuroguard.billing.entity.Invoice;
import com.neuroguard.billing.entity.InvoiceStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByStatus(InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByPatientId(UUID patientId, Pageable pageable);

    Page<Invoice> findByPatientIdAndStatus(UUID patientId, InvoiceStatus status, Pageable pageable);

    /**
     * Row lock for status changes (cancel, pay): two concurrent requests on the same invoice are serialised,
     * so an invoice can never be both cancelled and paid.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> findForUpdate(@Param("id") UUID id);
}
