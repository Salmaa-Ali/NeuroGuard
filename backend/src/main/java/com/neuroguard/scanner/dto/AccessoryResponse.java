package com.neuroguard.scanner.dto;

import com.neuroguard.scanner.entity.ScannerAccessory;

import java.util.UUID;

public record AccessoryResponse(UUID id, String name, String type, String serialNumber) {

    public static AccessoryResponse from(ScannerAccessory a) {
        return new AccessoryResponse(a.getId(), a.getName(), a.getType(), a.getSerialNumber());
    }
}
