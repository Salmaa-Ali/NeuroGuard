package com.neuroguard.scanner.repository;

import com.neuroguard.scanner.entity.QaTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface QaTestRepository extends JpaRepository<QaTest, UUID> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    List<QaTest> findAllByOrderByCodeAsc();

    List<QaTest> findByActiveTrueOrderByCodeAsc();
}
