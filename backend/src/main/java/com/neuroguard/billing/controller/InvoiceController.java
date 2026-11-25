package com.neuroguard.billing.controller;

import com.neuroguard.billing.dto.CreateInvoiceRequest;
import com.neuroguard.billing.dto.InvoiceResponse;
import com.neuroguard.billing.entity.InvoiceStatus;
import com.neuroguard.billing.service.InvoiceService;
import com.neuroguard.common.PageResponse;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Billing - Invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    /** Prices and totals are computed by the server; the body carries services and quantities only. */
    @PostMapping("/invoices")
    @PreAuthorize(Authz.BILLING_WRITE)
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse created = invoiceService.create(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    /** All invoices, newest first, optionally filtered by status. */
    @GetMapping("/invoices")
    @PreAuthorize(Authz.INVOICE_READ_ALL)
    public PageResponse<InvoiceResponse> list(@RequestParam(required = false) InvoiceStatus status,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return invoiceService.list(status, page, size);
    }

    @GetMapping("/patients/{patientId}/invoices")
    @PreAuthorize(Authz.INVOICE_READ_BY_PATIENT)
    public PageResponse<InvoiceResponse> listForPatient(@PathVariable UUID patientId,
                                                        @RequestParam(required = false) InvoiceStatus status,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "20") int size) {
        return invoiceService.listForPatient(patientId, status, page, size);
    }

    @GetMapping("/invoices/{id}")
    @PreAuthorize(Authz.INVOICE_READ_BY_RECORD)
    public InvoiceResponse get(@PathVariable UUID id) {
        return invoiceService.get(id);
    }

    /** UNPAID -> CANCELLED. PAID and CANCELLED are final (409). */
    @PostMapping("/invoices/{id}/cancel")
    @PreAuthorize(Authz.BILLING_WRITE)
    public InvoiceResponse cancel(@PathVariable UUID id) {
        return invoiceService.cancel(id);
    }
}
