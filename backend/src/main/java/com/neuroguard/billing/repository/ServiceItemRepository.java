package com.neuroguard.billing.repository;

import com.neuroguard.billing.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {

    /** Same name ignoring case and surrounding spaces. */
    @Query("SELECT count(s) > 0 FROM ServiceItem s WHERE lower(trim(s.name)) = lower(trim(:name))")
    boolean existsByNormalisedName(@Param("name") String name);

    /** As above, ignoring the entry being updated. */
    @Query("SELECT count(s) > 0 FROM ServiceItem s WHERE lower(trim(s.name)) = lower(trim(:name)) AND s.id <> :id")
    boolean existsByNormalisedNameAndIdNot(@Param("name") String name, @Param("id") UUID id);
}
