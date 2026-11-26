package com.neuroguard.scanner.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.scanner.dto.AccessoryRequest;
import com.neuroguard.scanner.dto.AccessoryResponse;
import com.neuroguard.scanner.dto.ScannerRequest;
import com.neuroguard.scanner.entity.ScannerAccessory;
import com.neuroguard.scanner.entity.ScannerDevice;
import com.neuroguard.scanner.entity.ScannerStatus;
import com.neuroguard.scanner.repository.ScannerAccessoryRepository;
import com.neuroguard.scanner.repository.ScannerDeviceRepository;
import com.neuroguard.security.CurrentUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Scanner inventory and coils (biomedical engineer writes). */
@Service
public class ScannerService {

    static final String ENTITY = "SCANNER";

    private final ScannerDeviceRepository scanners;
    private final ScannerAccessoryRepository accessories;
    private final AuditService auditService;

    public ScannerService(ScannerDeviceRepository scanners, ScannerAccessoryRepository accessories,
                          AuditService auditService) {
        this.scanners = scanners;
        this.accessories = accessories;
        this.auditService = auditService;
    }

    @Transactional
    public UUID create(ScannerRequest request) {
        String serial = request.serialNumber().trim();
        if (scanners.existsBySerialNumber(serial)) {
            throw new ConflictException("A scanner with serial number '" + serial + "' already exists");
        }
        ScannerDevice scanner = new ScannerDevice();
        apply(scanner, request, serial);
        scanner = scanners.saveAndFlush(scanner);
        audit(AuditAction.SCANNER_CREATED, scanner.getId());
        return scanner.getId();
    }

    @Transactional
    public void update(UUID id, ScannerRequest request) {
        ScannerDevice scanner = find(id);
        String serial = request.serialNumber().trim();
        if (scanners.existsBySerialNumberAndIdNot(serial, id)) {
            throw new ConflictException("A scanner with serial number '" + serial + "' already exists");
        }
        apply(scanner, request, serial);
        scanners.saveAndFlush(scanner);
        audit(AuditAction.SCANNER_UPDATED, id);
    }

    /** Status is administrative only; it never changes recorded sessions or any patient data. */
    @Transactional
    public void changeStatus(UUID id, ScannerStatus status) {
        ScannerDevice scanner = find(id);
        scanner.setStatus(status);
        scanners.saveAndFlush(scanner);
        audit(AuditAction.SCANNER_STATUS_CHANGED, id);
    }

    @Transactional
    public AccessoryResponse addAccessory(UUID scannerId, AccessoryRequest request) {
        ScannerAccessory accessory = new ScannerAccessory();
        accessory.setScanner(find(scannerId));
        applyAccessory(accessory, request);
        accessory = accessories.saveAndFlush(accessory);
        audit(AuditAction.SCANNER_UPDATED, scannerId);
        return AccessoryResponse.from(accessory);
    }

    @Transactional
    public AccessoryResponse updateAccessory(UUID scannerId, UUID accessoryId, AccessoryRequest request) {
        ScannerAccessory accessory = findAccessory(scannerId, accessoryId);
        applyAccessory(accessory, request);
        accessory = accessories.saveAndFlush(accessory);
        audit(AuditAction.SCANNER_UPDATED, scannerId);
        return AccessoryResponse.from(accessory);
    }

    @Transactional
    public void deleteAccessory(UUID scannerId, UUID accessoryId) {
        accessories.delete(findAccessory(scannerId, accessoryId));
        audit(AuditAction.SCANNER_UPDATED, scannerId);
    }

    public ScannerDevice find(UUID id) {
        return scanners.findById(id).orElseThrow(() -> new ResourceNotFoundException("Scanner", id));
    }

    private ScannerAccessory findAccessory(UUID scannerId, UUID accessoryId) {
        find(scannerId);
        return accessories.findByIdAndScannerId(accessoryId, scannerId)
                .orElseThrow(() -> new ResourceNotFoundException("Accessory", accessoryId));
    }

    private static void apply(ScannerDevice s, ScannerRequest r, String serial) {
        s.setName(r.name().trim());
        s.setManufacturer(r.manufacturer().trim());
        s.setModel(r.model().trim());
        s.setSerialNumber(serial);
        s.setStationName(blankToNull(r.stationName()));
        s.setFieldStrength(r.fieldStrength());
        s.setLocation(blankToNull(r.location()));
        s.setInstallDate(r.installDate());
        s.setCalibrationIntervalDays(r.calibrationIntervalDays());
    }

    private static void applyAccessory(ScannerAccessory a, AccessoryRequest r) {
        a.setName(r.name().trim());
        a.setType(blankToNull(r.type()));
        a.setSerialNumber(blankToNull(r.serialNumber()));
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private void audit(String action, UUID scannerId) {
        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), action, ENTITY, scannerId);
    }
}
