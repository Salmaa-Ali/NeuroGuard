package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.ScannerStatus;
import jakarta.validation.constraints.NotNull;

public record ScannerStatusRequest(@NotNull(message = "is required") ScannerStatus status) {
}
