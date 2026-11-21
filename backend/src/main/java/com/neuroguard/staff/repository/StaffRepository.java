package com.neuroguard.staff.repository;

import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.Staff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

    Optional<Staff> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByBaseRole(BaseRole baseRole);

    long countByBaseRole(BaseRole baseRole);
}
