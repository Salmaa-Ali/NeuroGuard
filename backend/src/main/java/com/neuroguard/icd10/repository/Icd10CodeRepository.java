package com.neuroguard.icd10.repository;

import com.neuroguard.icd10.entity.Icd10Code;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Icd10CodeRepository extends JpaRepository<Icd10Code, String> {

    /**
     * Code starts with {@code codePrefix} or the description contains {@code descriptionPattern}
     * (case-insensitive). Both arguments are LIKE patterns already escaped with backslash.
     */
    @Query("""
            SELECT c FROM Icd10Code c
            WHERE c.code LIKE :codePrefix ESCAPE '\\'
               OR lower(c.description) LIKE :descriptionPattern ESCAPE '\\'
            """)
    Page<Icd10Code> search(@Param("codePrefix") String codePrefix,
                           @Param("descriptionPattern") String descriptionPattern,
                           Pageable pageable);
}
