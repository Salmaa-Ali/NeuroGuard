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

import java.util.UUID;

/** A coil or other accessory of a scanner. */
@Entity
@Table(name = "scanner_accessory")
@Getter
@Setter
@NoArgsConstructor
public class ScannerAccessory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "accessory_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scanner_id", nullable = false)
    private ScannerDevice scanner;

    @Column(nullable = false)
    private String name;

    @Column(length = 100)
    private String type;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;
}
