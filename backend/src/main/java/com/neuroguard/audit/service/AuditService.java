package com.neuroguard.audit.service;

import com.neuroguard.audit.dto.AuditLogResponse;
import com.neuroguard.audit.entity.ActorType;
import com.neuroguard.audit.entity.AuditLog;
import com.neuroguard.audit.repository.AuditLogRepository;
import com.neuroguard.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(String action, ActorType actorType, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        String normalizedAction = action == null ? null : action.trim();
        Page<AuditLogResponse> result = repository
                .findFiltered(normalizedAction, actorType, PageRequest.of(safePage, safeSize))
                .map(AuditLogResponse::from);
        return PageResponse.from(result);
    }

    /** Joins the caller's transaction: the audit row commits or rolls back with the business change. */
    @Transactional
    public void record(ActorType actorType, UUID actorId, String action, String entityType, UUID entityId) {
        repository.save(build(actorType, actorId, action, entityType, entityId));
    }

    /** Own transaction: survives even if the caller's work is rolled back (e.g. failed logins). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependent(ActorType actorType, UUID actorId, String action, String entityType, UUID entityId) {
        repository.save(build(actorType, actorId, action, entityType, entityId));
    }

    private AuditLog build(ActorType actorType, UUID actorId, String action, String entityType, UUID entityId) {
        AuditLog log = new AuditLog();
        log.setActorType(actorType);
        log.setActorId(actorId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        return log;
    }
}
