package com.neuroguard.billing.repository;

import com.neuroguard.billing.entity.InvoiceItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, UUID> {

    /** Items of a page of invoices in one query, with their services. */
    @EntityGraph(attributePaths = "service")
    List<InvoiceItem> findByInvoiceIdInOrderByCreatedAtAscIdAsc(Collection<UUID> invoiceIds);
}
