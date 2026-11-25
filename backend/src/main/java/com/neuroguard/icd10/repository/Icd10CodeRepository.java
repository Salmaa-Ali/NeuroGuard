package com.neuroguard.icd10.repository;

import com.neuroguard.icd10.entity.Icd10Code;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Icd10CodeRepository extends JpaRepository<Icd10Code, String> {
}
