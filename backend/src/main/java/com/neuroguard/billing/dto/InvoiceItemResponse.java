package com.neuroguard.billing.dto;

import com.neuroguard.billing.entity.InvoiceItem;

import java.math.BigDecimal;
import java.util.UUID;

/** {@code unitPrice} is the price at invoicing time, not today's catalogue price. */
public record InvoiceItemResponse(
        UUID id,
        UUID serviceId,
        String serviceName,
        String icd10Code,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public static InvoiceItemResponse from(InvoiceItem i) {
        return new InvoiceItemResponse(i.getId(), i.getService().getId(), i.getService().getName(), i.getIcd10Code(),
                i.getQuantity(), i.getUnitPrice(), i.getLineTotal());
    }
}
