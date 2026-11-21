package com.neuroguard.scanner.repository;

import com.neuroguard.scanner.entity.CalibrationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CalibrationSessionRepository extends JpaRepository<CalibrationSession, UUID> {
}
