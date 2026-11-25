package com.neuroguard.billing.controller;

import com.neuroguard.billing.dto.ServiceRequest;
import com.neuroguard.billing.dto.ServiceResponse;
import com.neuroguard.billing.service.ServiceCatalogueService;
import com.neuroguard.common.PageResponse;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/services")
@Tag(name = "Billing - Service catalogue")
public class ServiceCatalogueController {

    private final ServiceCatalogueService catalogueService;

    public ServiceCatalogueController(ServiceCatalogueService catalogueService) {
        this.catalogueService = catalogueService;
    }

    @PostMapping
    @PreAuthorize(Authz.BILLING_WRITE)
    public ResponseEntity<ServiceResponse> create(@Valid @RequestBody ServiceRequest request) {
        ServiceResponse created = catalogueService.create(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    /** Replaces name and price. Existing invoices keep the price they were billed at. */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.BILLING_WRITE)
    public ServiceResponse update(@PathVariable UUID id, @Valid @RequestBody ServiceRequest request) {
        return catalogueService.update(id, request);
    }

    /** Sorted by name. */
    @GetMapping
    @PreAuthorize(Authz.ANY_STAFF)
    public PageResponse<ServiceResponse> list(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return catalogueService.list(page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize(Authz.ANY_STAFF)
    public ServiceResponse get(@PathVariable UUID id) {
        return catalogueService.get(id);
    }
}
