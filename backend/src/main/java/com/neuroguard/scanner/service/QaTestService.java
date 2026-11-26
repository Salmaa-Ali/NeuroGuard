package com.neuroguard.scanner.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.common.BadRequestException;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.scanner.dto.QaTestRequest;
import com.neuroguard.scanner.dto.QaTestResponse;
import com.neuroguard.scanner.entity.QaTest;
import com.neuroguard.scanner.repository.QaTestRepository;
import com.neuroguard.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * QA tests and their tolerances. Editing a test never changes recorded sessions: every measurement keeps
 * a copy of the limits that applied when it was recorded. Tests are deactivated, never deleted.
 */
@Service
public class QaTestService {

    private final QaTestRepository repository;
    private final AuditService auditService;

    public QaTestService(QaTestRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<QaTestResponse> list(boolean activeOnly) {
        List<QaTest> tests = activeOnly ? repository.findByActiveTrueOrderByCodeAsc() : repository.findAllByOrderByCodeAsc();
        return tests.stream().map(QaTestResponse::from).toList();
    }

    @Transactional
    public QaTestResponse create(QaTestRequest request) {
        String code = normaliseCode(request.code());
        if (repository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("A QA test with code '" + code + "' already exists");
        }
        QaTest test = new QaTest();
        apply(test, request, code);
        test = repository.saveAndFlush(test);
        audit(test.getId());
        return QaTestResponse.from(test);
    }

    @Transactional
    public QaTestResponse update(UUID id, QaTestRequest request) {
        QaTest test = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("QA test", id));
        String code = normaliseCode(request.code());
        if (repository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new ConflictException("A QA test with code '" + code + "' already exists");
        }
        apply(test, request, code);
        test = repository.saveAndFlush(test);
        audit(test.getId());
        return QaTestResponse.from(test);
    }

    private static void apply(QaTest test, QaTestRequest r, String code) {
        if (r.lowerLimit() != null && r.upperLimit() != null && r.lowerLimit().compareTo(r.upperLimit()) > 0) {
            throw new BadRequestException("lowerLimit must not be greater than upperLimit", "lowerLimit");
        }
        test.setCode(code);
        test.setName(r.name().trim());
        test.setUnit(ScannerService.blankToNull(r.unit()));
        test.setLowerLimit(r.lowerLimit());
        test.setUpperLimit(r.upperLimit());
        test.setReference(ScannerService.blankToNull(r.reference()));
        test.setPlaceholder(Boolean.TRUE.equals(r.placeholder()));
        test.setActive(r.active() == null || r.active());
    }

    private static String normaliseCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void audit(UUID testId) {
        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.QA_TEST_CHANGED, "QA_TEST", testId);
    }
}
