package com.neuroguard.imaging.repository;

import com.neuroguard.imaging.entity.MriOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MriOrderRepository extends JpaRepository<MriOrder, UUID> {

    Optional<MriOrder> findByAccessionNumber(String accessionNumber);
    Page<MriOrder> findAllBy(Pageable pageable);
    Page<MriOrder> findByPatientId(UUID patientId, Pageable pageable);

    /** Same approach as patient_number_seq; nextval is not rolled back, so a failed create leaves a gap. */
    @Query(value = "SELECT nextval('accession_number_seq')", nativeQuery = true)
    Long nextAccessionNumberSeq();
}
