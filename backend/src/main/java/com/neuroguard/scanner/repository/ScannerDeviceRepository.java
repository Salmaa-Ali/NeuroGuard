package com.neuroguard.scanner.repository;

import com.neuroguard.scanner.entity.ScannerDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScannerDeviceRepository extends JpaRepository<ScannerDevice, UUID> {

    boolean existsBySerialNumber(String serialNumber);
}
