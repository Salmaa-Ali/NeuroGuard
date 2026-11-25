package com.neuroguard.billing.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.billing.dto.PaymentResponse;
import com.neuroguard.billing.entity.Invoice;
import com.neuroguard.billing.entity.InvoiceStatus;
import com.neuroguard.billing.entity.Payment;
import com.neuroguard.billing.entity.PaymentStatus;
import com.neuroguard.billing.repository.InvoiceRepository;
import com.neuroguard.billing.repository.PaymentRepository;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.common.PatientRecordAccess;
import com.neuroguard.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Simulated payments: no payment provider, no card or account data. Paying always succeeds for the full
 * invoice total; the point is the invoice status flow and the revenue figures, not payment processing.
 */
@Service
public class PaymentService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("paymentDate"), Sort.Order.asc("id"));

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public PaymentService(InvoiceRepository invoiceRepository, PaymentRepository paymentRepository,
                          AuditService auditService) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    /**
     * Payment row, invoice status and audit entry commit together or not at all, so there is never a PAID
     * invoice without its payment (or the reverse). The invoice row is locked first, which serialises this
     * with a concurrent cancel or a double click on "pay". Ownership is checked before the status, so a
     * patient learns nothing about someone else's invoice, not even that it is already paid.
     */
    @Transactional
    public PaymentResponse pay(UUID invoiceId) {
        Invoice invoice = PatientRecordAccess.requireReadable(invoiceRepository.findForUpdate(invoiceId),
                inv -> inv.getPatient().getId(), "Invoice", invoiceId);
        if (invoice.getStatus() != InvoiceStatus.UNPAID) {
            throw new ConflictException("Only UNPAID invoices can be paid; this invoice is " + invoice.getStatus());
        }
        if (invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ConflictException("This invoice has a total of 0.00, so there is nothing to pay");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmountPaid(Money.of(invoice.getTotalAmount()));
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaymentDate(Instant.now().truncatedTo(ChronoUnit.MICROS));
        payment = paymentRepository.save(payment);

        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.saveAndFlush(invoice);

        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.PAYMENT_COMPLETED, "PAYMENT", payment.getId());
        return PaymentResponse.from(payment);
    }

    /** Clerk and admin overview of all payments, newest first. */
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> list(int page, int size) {
        return PageResponse.of(paymentRepository.findAll(Pages.of(page, size, NEWEST_FIRST)), PaymentResponse::from);
    }
}
