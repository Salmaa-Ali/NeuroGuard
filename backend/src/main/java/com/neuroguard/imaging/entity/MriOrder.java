package com.neuroguard.imaging.entity;

import com.neuroguard.appointment.entity.Appointment;
import com.neuroguard.common.BaseEntity;
import com.neuroguard.patient.entity.Patient;
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

import java.util.UUID;

@Entity
@Table(name = "mri_order")
@Getter
@Setter
@NoArgsConstructor
public class MriOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "accession_number", nullable = false, unique = true, length = 64)
    private String accessionNumber;

    @Column(columnDefinition = "text")
    private String indication;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MriOrderStatus status = MriOrderStatus.ORDERED;
}
