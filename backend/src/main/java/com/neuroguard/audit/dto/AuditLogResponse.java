package com.neuroguard.audit.dto;

import com.neuroguard.audit.entity.ActorType;
import com.neuroguard.audit.entity.AuditLog;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        UUID actorId,
        ActorType actorType,
        String action,
        String entityType,
        UUID entityId,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActorType(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getCreatedAt()
        );
    }
}
