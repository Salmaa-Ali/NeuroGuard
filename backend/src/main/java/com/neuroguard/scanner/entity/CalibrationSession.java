package com.neuroguard.scanner.entity;

import com.neuroguard.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(length = 100)
    private String result;

    @Column(name = "next_due_date")
    private LocalDate nextDueDate;

    @Column(name = "risk_score")
    private Double riskScore;
}
