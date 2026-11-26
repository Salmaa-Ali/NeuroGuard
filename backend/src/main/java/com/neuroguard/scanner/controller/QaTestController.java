package com.neuroguard.scanner.controller;

import com.neuroguard.scanner.dto.QaTestRequest;
import com.neuroguard.scanner.dto.QaTestResponse;
import com.neuroguard.scanner.service.QaTestService;
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

import java.util.List;
import java.util.UUID;

/** QA tests and tolerances. Tolerances are entered by the biomedical engineer; placeholders are flagged. */
@RestController
@RequestMapping("/api/qa-tests")
@Tag(name = "Scanners & QA")
public class QaTestController {

    private final QaTestService qaTestService;

    public QaTestController(QaTestService qaTestService) {
        this.qaTestService = qaTestService;
    }

    /** Sorted by code. activeOnly=true hides deactivated tests. */
    @GetMapping
    @PreAuthorize(Authz.SCANNER_READ)
    public List<QaTestResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return qaTestService.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize(Authz.SCANNER_WRITE)
    public ResponseEntity<QaTestResponse> create(@Valid @RequestBody QaTestRequest request) {
        QaTestResponse created = qaTestService.create(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    /** Replaces the test. Send active=false to deactivate (tests are never deleted). */
    @PutMapping("/{id}")
    @PreAuthorize(Authz.SCANNER_WRITE)
    public QaTestResponse update(@PathVariable UUID id, @Valid @RequestBody QaTestRequest request) {
        return qaTestService.update(id, request);
    }
}
