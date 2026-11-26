package com.neuroguard.scanner.repository;

import com.neuroguard.scanner.entity.ScannerAccessory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScannerAccessoryRepository extends JpaRepository<ScannerAccessory, UUID> {

    List<ScannerAccessory> findByScannerIdOrderByNameAscIdAsc(UUID scannerId);

    Optional<ScannerAccessory> findByIdAndScannerId(UUID id, UUID scannerId);
}
