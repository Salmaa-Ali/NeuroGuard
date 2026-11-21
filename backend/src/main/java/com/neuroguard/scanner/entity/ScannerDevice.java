package com.neuroguard.scanner.entity;

import com.neuroguard.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
}
