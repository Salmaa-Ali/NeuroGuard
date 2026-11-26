package com.neuroguard.scanner.controller;

import com.neuroguard.common.PageResponse;
import com.neuroguard.scanner.dto.AccessoryRequest;
import com.neuroguard.scanner.dto.AccessoryResponse;
import com.neuroguard.scanner.dto.CalibrationDetail;
import com.neuroguard.scanner.dto.CalibrationSummary;
import com.neuroguard.scanner.dto.QaOverview;
import com.neuroguard.scanner.dto.QaSummary;
import com.neuroguard.scanner.dto.RecordCalibrationRequest;
import com.neuroguard.scanner.dto.ScannerDetail;
import com.neuroguard.scanner.dto.ScannerListItem;
import com.neuroguard.scanner.dto.ScannerRequest;
import com.neuroguard.scanner.dto.ScannerStatusRequest;
import com.neuroguard.scanner.entity.QaStatus;
import com.neuroguard.scanner.entity.ScannerStatus;
import com.neuroguard.scanner.service.CalibrationService;
import com.neuroguard.scanner.service.ScannerQueryService;
import com.neuroguard.scanner.service.ScannerService;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Scanner QA. A FAILED or OVERDUE status means "Flagged for review" only: it never blocks, invalidates or
 * changes any patient study or report. Risk scores are indicative.
 */
@RestController
@RequestMapping("/api/scanners")
@Tag(name = "Scanners & QA")
public class ScannerController {

    private final ScannerService scannerService;
    private final ScannerQueryService queryService;
    private final CalibrationService calibrationService;

    public ScannerController(ScannerService scannerService, ScannerQueryService queryService,
                             CalibrationService calibrationService) {
        this.scannerService = scannerService;
        this.queryService = queryService;
        this.calibrationService = calibrationService;
    }

    // ---- read ----

    /** Sorted by name. Optional filters: status, qaStatus. */
    @GetMapping
    @PreAuthorize(Authz.SCANNER_READ)
    public PageResponse<ScannerListItem> list(@RequestParam(required = false) ScannerStatus status,
                                              @RequestParam(required = false) QaStatus qaStatus,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "50") int size) {
        return queryService.list(status, qaStatus, page, size);
    }

    /** Counts per QA status (RETIRED scanners excluded), for the dashboard. */
    @GetMapping("/qa-overview")
    @PreAuthorize(Authz.SCANNER_READ)
    public QaOverview overview() {
        return queryService.overview();
    }

    @GetMapping("/{id}")
    @PreAuthorize(Authz.SCANNER_READ)
    public ScannerDetail get(@PathVariable UUID id) {
        return queryService.detail(id);
    }

    /** Compact read-only QA status for the viewer's equipment panel. */
    @GetMapping("/{id}/qa-summary")
    @PreAuthorize(Authz.SCANNER_READ)
    public QaSummary qaSummary(@PathVariable UUID id) {
        return queryService.qaSummary(id);
    }

    /** Newest first. */
    @GetMapping("/{id}/calibrations")
    @PreAuthorize(Authz.SCANNER_READ)
    public PageResponse<CalibrationSummary> history(@PathVariable UUID id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "20") int size) {
        return calibrationService.history(id, page, size);
    }

    // ---- write (biomedical engineer) ----

    @PostMapping
    @PreAuthorize(Authz.SCANNER_WRITE)
    public ResponseEntity<ScannerDetail> create(@Valid @RequestBody ScannerRequest request) {
        UUID id = scannerService.create(request);
        return ResponseEntity.created(location("/{id}", id)).body(queryService.detail(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Authz.SCANNER_WRITE)
    public ScannerDetail update(@PathVariable UUID id, @Valid @RequestBody ScannerRequest request) {
        scannerService.update(id, request);
        return queryService.detail(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize(Authz.SCANNER_WRITE)
    public ScannerDetail changeStatus(@PathVariable UUID id, @Valid @RequestBody ScannerStatusRequest request) {
        scannerService.changeStatus(id, request.status());
        return queryService.detail(id);
    }

    @PostMapping("/{id}/accessories")
    @PreAuthorize(Authz.SCANNER_WRITE)
    public ResponseEntity<AccessoryResponse> addAccessory(@PathVariable UUID id,
                                                          @Valid @RequestBody AccessoryRequest request) {
        AccessoryResponse created = scannerService.addAccessory(id, request);
        return ResponseEntity.created(location("/{aid}", created.id())).body(created);
    }

    @PutMapping("/{id}/accessories/{accessoryId}")
    @PreAuthorize(Authz.SCANNER_WRITE)
    public AccessoryResponse updateAccessory(@PathVariable UUID id, @PathVariable UUID accessoryId,
                                             @Valid @RequestBody AccessoryRequest request) {
        return scannerService.updateAccessory(id, accessoryId, request);
    }

    @DeleteMapping("/{id}/accessories/{accessoryId}")
    @PreAuthorize(Authz.SCANNER_WRITE)
    public ResponseEntity<Void> deleteAccessory(@PathVariable UUID id, @PathVariable UUID accessoryId) {
        scannerService.deleteAccessory(id, accessoryId);
        return ResponseEntity.noContent().build();
    }

    /** The server computes result, next due date and the indicative risk score; performed_by is the caller. */
    @PostMapping("/{id}/calibrations")
    @PreAuthorize(Authz.SCANNER_WRITE)
    public ResponseEntity<CalibrationDetail> recordCalibration(@PathVariable UUID id,
                                                               @Valid @RequestBody RecordCalibrationRequest request) {
        CalibrationDetail created = calibrationService.record(id, request);
        URI uri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/calibrations/{sid}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(uri).body(created);
    }

    private static URI location(String suffix, UUID id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path(suffix).buildAndExpand(id).toUri();
    }
}
