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

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A QA check and its tolerance. Tolerances are data entered by the biomedical engineer, never defaults in
 * code. A null limit means no bound on that side. {@code placeholder} marks values still to be confirmed by a
 * medical physicist.
 */
@Entity
@Table(name = "qa_test")
@Getter
@Setter
@NoArgsConstructor
public class QaTest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "qa_test_id")
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(length = 30)
    private String unit;

    @Column(name = "lower_limit", precision = 12, scale = 4)
    private BigDecimal lowerLimit;

    @Column(name = "upper_limit", precision = 12, scale = 4)
    private BigDecimal upperLimit;

    /** Where the tolerance comes from (protocol, physicist sign-off, ...). */
    @Column(columnDefinition = "text")
    private String reference;

    @Column(name = "is_placeholder", nullable = false)
    private boolean placeholder;

    @Column(nullable = false)
    private boolean active = true;
}
