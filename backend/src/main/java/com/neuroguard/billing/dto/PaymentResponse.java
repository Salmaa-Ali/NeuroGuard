package com.neuroguard.billing.dto;

import com.neuroguard.billing.entity.Payment;
import com.neuroguard.billing.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A simulated payment. There are no card or account fields anywhere in the model. */
public record PaymentResponse(
        UUID id,
        UUID invoiceId,
        UUID patientId,
        BigDecimal amountPaid,
        PaymentStatus status,
        Instant paymentDate) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getInvoice().getId(), p.getInvoice().getPatient().getId(),
                p.getAmountPaid(), p.getStatus(), p.getPaymentDate());
    }
}
