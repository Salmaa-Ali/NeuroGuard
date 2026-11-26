package com.neuroguard.appointment.repository;

import com.neuroguard.appointment.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);
    Page<Appointment> findAllBy(Pageable pageable);
    @Query(value = "select exists(select 1 from appointment a where a.staff_id = :staffId and a.status = 'SCHEDULED' and a.scheduled_time < :end and a.scheduled_time + a.duration_minutes * interval '1 minute' > :start)", nativeQuery = true)
    boolean staffHasConflict(@Param("staffId") UUID staffId, @Param("start") java.time.Instant start,
                             @Param("end") java.time.Instant end);

    @Query(value = "select exists(select 1 from appointment a where a.patient_id = :patientId and a.status = 'SCHEDULED' and a.scheduled_time < :end and a.scheduled_time + a.duration_minutes * interval '1 minute' > :start)", nativeQuery = true)
    boolean patientHasConflict(@Param("patientId") UUID patientId, @Param("start") java.time.Instant start,
                               @Param("end") java.time.Instant end);
}
