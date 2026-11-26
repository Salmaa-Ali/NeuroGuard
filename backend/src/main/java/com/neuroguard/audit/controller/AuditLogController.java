package com.neuroguard.audit.controller;

import com.neuroguard.audit.dto.AuditLogResponse;
import com.neuroguard.audit.entity.ActorType;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.common.PageResponse;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize(Authz.ADMIN_ONLY)
@Tag(name = "Admin - Audit log")
public class AuditLogController {

    private final AuditService auditService;

    public AuditLogController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> list(@RequestParam(required = false) String action,
                                               @RequestParam(required = false) ActorType actorType,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return auditService.list(action, actorType, page, size);
    }
}
