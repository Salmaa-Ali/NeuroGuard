package com.neuroguard.scanner.entity;

import com.neuroguard.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "scanner_device")
@Getter
@Setter
@NoArgsConstructor
public class ScannerDevice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "scanner_id")
    private UUID id;

    /** Display name, e.g. "MRI-1 (1.5 T)". */
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String manufacturer;

    @Column(nullable = false)
    private String model;

    @Column(name = "serial_number", nullable = false, unique = true, length = 100)
    private String serialNumber;

    @Column(name = "station_name")
    private String stationName;

    /** Magnetic field strength in tesla (e.g. 1.5, 3.0). */
    @Column(name = "field_strength", nullable = false)
    private double fieldStrength;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ScannerStatus status = ScannerStatus.ACTIVE;

    private String location;

    @Column(name = "install_date")
    private LocalDate installDate;

    /** Days between QA sessions; next_due_date = session date + this. */
    @Column(name = "calibration_interval_days", nullable = false)
    private int calibrationIntervalDays;
}
