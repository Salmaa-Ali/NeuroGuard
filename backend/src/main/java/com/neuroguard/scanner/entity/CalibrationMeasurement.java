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

import java.math.BigDecimal;
import java.util.UUID;

/** One measured value. The limits are a copy of the qa_test limits at the time it was recorded. */
@Entity
@Table(name = "calibration_measurement")
@Getter
@Setter
@NoArgsConstructor
public class CalibrationMeasurement extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "measurement_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private CalibrationSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "qa_test_id", nullable = false)
    private QaTest qaTest;

    @Column(name = "measured_value", nullable = false, precision = 12, scale = 4)
    private BigDecimal measuredValue;

    @Column(name = "lower_limit", precision = 12, scale = 4)
    private BigDecimal lowerLimit;

    @Column(name = "upper_limit", precision = 12, scale = 4)
    private BigDecimal upperLimit;

    @Column(name = "within_tolerance", nullable = false)
    private boolean withinTolerance;
}
