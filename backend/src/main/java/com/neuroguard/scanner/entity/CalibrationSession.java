package com.neuroguard.scanner.entity;

import com.neuroguard.common.BaseEntity;
import com.neuroguard.staff.entity.Staff;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One QA session. result, next_due_date and risk_score are computed by the server, never sent by clients. */
@Entity
@Table(name = "calibration_session")
@Getter
@Setter
@NoArgsConstructor
public class CalibrationSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scanner_id", nullable = false)
    private ScannerDevice scanner;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "performed_by", nullable = false)
    private Staff performedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CalibrationResult result;

    @Column(name = "next_due_date", nullable = false)
    private LocalDate nextDueDate;

    /** Indicative 0-100 score stored when the session was recorded. Not a safety verdict. */
    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "certificate_ref", length = 100)
    private String certificateRef;
}
