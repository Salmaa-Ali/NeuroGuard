package com.neuroguard.billing.controller;

import com.neuroguard.billing.dto.PaymentResponse;
import com.neuroguard.billing.service.PaymentService;
import com.neuroguard.common.BadRequestException;
import com.neuroguard.common.PageResponse;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Billing - Payments (simulated)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Pays the full invoice total. Takes no request body: there are no card fields, and any body is rejected
     * with 400 so card data can never be sent here by mistake.
     */
    @Operation(summary = "Simulated payment of the full invoice total (no request body)")
    @PostMapping("/invoices/{id}/pay")
    @PreAuthorize(Authz.INVOICE_PAY)
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse pay(@PathVariable UUID id, @RequestBody(required = false) String body) {
        if (body != null && !body.isBlank()) {
            throw new BadRequestException("This endpoint takes no request body: payments are simulated "
                    + "and no card or account details are accepted");
        }
        return paymentService.pay(id);
    }

    /** All payments, newest first. */
    @GetMapping("/payments")
    @PreAuthorize(Authz.INVOICE_READ_ALL)
    public PageResponse<PaymentResponse> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return paymentService.list(page, size);
    }
}
