package com.neuroguard.scanner.controller;

import com.neuroguard.scanner.dto.CalibrationDetail;
import com.neuroguard.scanner.service.CalibrationService;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/calibrations")
@Tag(name = "Scanners & QA")
public class CalibrationController {

    private final CalibrationService calibrationService;

    public CalibrationController(CalibrationService calibrationService) {
        this.calibrationService = calibrationService;
    }

    /** One session with all its measurements (limits as they were when recorded). */
    @GetMapping("/{sessionId}")
    @PreAuthorize(Authz.SCANNER_READ)
    public CalibrationDetail get(@PathVariable UUID sessionId) {
        return calibrationService.get(sessionId);
    }
}
