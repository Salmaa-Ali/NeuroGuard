package com.neuroguard.clinical.entity;

import com.neuroguard.common.BaseEntity;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.staff.entity.Staff;
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
import java.util.UUID;

@Entity
@Table(name = "diagnosis")
@Getter
@Setter
@NoArgsConstructor
public class Diagnosis extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "diagnosis_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @Column(name = "icd10_code", nullable = false, length = 10)
    private String icd10Code;

    @Column(name = "diagnosed_at", nullable = false)
    private Instant diagnosedAt;

    /** Optional free-text clinical notes from the diagnosing neurologist. */
    @Column(columnDefinition = "text")
    private String notes;
}
