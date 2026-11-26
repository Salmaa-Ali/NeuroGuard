package com.neuroguard.scanner.repository;

import com.neuroguard.scanner.entity.CalibrationSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalibrationSessionRepository extends JpaRepository<CalibrationSession, UUID> {

    /** History, newest first. */
    @EntityGraph(attributePaths = "performedBy")
    Page<CalibrationSession> findByScannerIdOrderByPerformedAtDescIdDesc(UUID scannerId, Pageable pageable);

    /** The latest session of one scanner (by performed_at). */
    @EntityGraph(attributePaths = "performedBy")
    Optional<CalibrationSession> findFirstByScannerIdOrderByPerformedAtDescIdDesc(UUID scannerId);

    /** Sessions performed strictly before a moment, newest first (used for the risk window and lateness). */
    List<CalibrationSession> findByScannerIdAndPerformedAtBeforeOrderByPerformedAtDescIdDesc(
            UUID scannerId, Instant before, Pageable pageable);

    /** The latest session of every scanner in one query. */
    @Query("""
            SELECT s FROM CalibrationSession s
            WHERE s.performedAt = (SELECT max(s2.performedAt) FROM CalibrationSession s2 WHERE s2.scanner = s.scanner)
            """)
    List<CalibrationSession> findLatestPerScanner();

    @Query("SELECT s FROM CalibrationSession s JOIN FETCH s.scanner JOIN FETCH s.performedBy WHERE s.id = :id")
    Optional<CalibrationSession> findDetailed(@Param("id") UUID id);
}
