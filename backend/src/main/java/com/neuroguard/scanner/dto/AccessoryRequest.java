package com.neuroguard.scanner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccessoryRequest(
        @NotBlank(message = "must not be blank") @Size(max = 255) String name,
        @Size(max = 100) String type,
        @Size(max = 100) String serialNumber) {
}
