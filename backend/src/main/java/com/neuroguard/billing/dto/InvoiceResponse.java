package com.neuroguard.billing.dto;

import com.neuroguard.billing.entity.Invoice;
import com.neuroguard.billing.entity.InvoiceStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID patientId,
        UUID appointmentId,
        InvoiceStatus status,
        LocalDate issueDate,
        BigDecimal totalAmount,
        List<InvoiceItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    public static InvoiceResponse from(Invoice inv, List<InvoiceItemResponse> items) {
        return new InvoiceResponse(inv.getId(), inv.getPatient().getId(),
                inv.getAppointment() == null ? null : inv.getAppointment().getId(),
                inv.getStatus(), inv.getIssueDate(), inv.getTotalAmount(), items, inv.getCreatedAt(), inv.getUpdatedAt());
    }
}
