package com.neuroguard.billing.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.billing.dto.ServiceRequest;
import com.neuroguard.billing.dto.ServiceResponse;
import com.neuroguard.billing.entity.ServiceItem;
import com.neuroguard.billing.repository.ServiceItemRepository;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Billable services. Prices here are current list prices; invoices keep their own snapshot. */
@Service
public class ServiceCatalogueService {

    private static final Sort BY_NAME = Sort.by("name").and(Sort.by("id"));

    private final ServiceItemRepository repository;
    private final AuditService auditService;

    public ServiceCatalogueService(ServiceItemRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /**
     * Names are compared ignoring case and spaces because "Brain MRI" and "brain mri " would look like
     * the same line to a clerk picking from the list; the DB unique constraint only catches exact copies.
     */
    @Transactional
    public ServiceResponse create(ServiceRequest request) {
        String name = request.name().trim();
        if (repository.existsByNormalisedName(name)) {
            throw new ConflictException("A service named '" + name + "' already exists");
        }
        ServiceItem service = new ServiceItem();
        service.setName(name);
        service.setPrice(Money.of(request.price()));
        service = repository.saveAndFlush(service);

        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.SERVICE_CREATED, "SERVICE", service.getId());
        return ServiceResponse.from(service);
    }

    /**
     * Changing the price is safe for billing history: invoice lines store their own unit_price, so only
     * invoices created after this update use the new price.
     */
    @Transactional
    public ServiceResponse update(UUID id, ServiceRequest request) {
        ServiceItem service = find(id);
        String name = request.name().trim();
        if (repository.existsByNormalisedNameAndIdNot(name, id)) {
            throw new ConflictException("A service named '" + name + "' already exists");
        }
        service.setName(name);
        service.setPrice(Money.of(request.price()));
        service = repository.saveAndFlush(service);

        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.SERVICE_UPDATED, "SERVICE", service.getId());
        return ServiceResponse.from(service);
    }

    @Transactional(readOnly = true)
    public PageResponse<ServiceResponse> list(int page, int size) {
        return PageResponse.of(repository.findAll(Pages.of(page, size, BY_NAME)), ServiceResponse::from);
    }

    @Transactional(readOnly = true)
    public ServiceResponse get(UUID id) {
        return ServiceResponse.from(find(id));
    }

    /** Used by invoicing to read the current price; 404 if the service does not exist. */
    @Transactional(readOnly = true)
    public ServiceItem find(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Service", id));
    }
}
