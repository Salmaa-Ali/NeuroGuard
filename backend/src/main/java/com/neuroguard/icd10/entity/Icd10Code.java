package com.neuroguard.icd10.entity;

import com.neuroguard.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ICD-10 reference code. The natural key is the code itself (upper case, no dot, as in the CMS
 * ICD-10-CM files), so diagnosis.icd10_code and invoice_item.icd10_code reference it directly.
 */
@Entity
@Table(name = "icd10_code")
@Getter
@Setter
@NoArgsConstructor
public class Icd10Code extends BaseEntity {

    @Id
    @Column(length = 10)
    private String code;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    /** First three characters of the code, e.g. G40 for G40909. */
    @Column(nullable = false, length = 3)
    private String category;

    public Icd10Code(String code, String description) {
        this.code = code;
        this.description = description;
        this.category = code.substring(0, Math.min(3, code.length()));
    }
}
