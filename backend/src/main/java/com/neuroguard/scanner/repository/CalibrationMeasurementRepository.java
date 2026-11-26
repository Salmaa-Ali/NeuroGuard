package com.neuroguard.scanner.repository;

import com.neuroguard.scanner.entity.CalibrationMeasurement;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CalibrationMeasurementRepository extends JpaRepository<CalibrationMeasurement, UUID> {

    @EntityGraph(attributePaths = "qaTest")
    List<CalibrationMeasurement> findBySessionIdIn(Collection<UUID> sessionIds);
}
