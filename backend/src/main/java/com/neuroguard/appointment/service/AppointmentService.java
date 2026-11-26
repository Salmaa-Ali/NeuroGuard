package com.neuroguard.appointment.service;

import com.neuroguard.appointment.dto.*;
import com.neuroguard.appointment.entity.*;
import com.neuroguard.appointment.repository.AppointmentRepository;
import com.neuroguard.common.*;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.CurrentUser;
import com.neuroguard.staff.repository.StaffRepository;
import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class AppointmentService {
    private final AppointmentRepository appointments;
    private final PatientRepository patients;
    private final StaffRepository staff;
    public AppointmentService(AppointmentRepository appointments, PatientRepository patients, StaffRepository staff) {
        this.appointments = appointments; this.patients = patients; this.staff = staff;
    }
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> list(UUID patientId, int page, int size) {
        var pageable = Pages.of(page, size, Sort.by(Sort.Order.asc("scheduledTime"), Sort.Order.asc("id")));
        if (patientId != null && !patients.existsById(patientId)) throw new ResourceNotFoundException("Patient", patientId);
        return PageResponse.of(patientId == null ? appointments.findAllBy(pageable) : appointments.findByPatientId(patientId, pageable), AppointmentResponse::from);
    }
    @Transactional
    public AppointmentResponse create(CreateAppointmentRequest request) {
        var patient = patients.findById(request.patientId()).orElseThrow(() -> new ResourceNotFoundException("Patient", request.patientId()));
        UUID staffId = CurrentUser.require().id();
        var clinician = staff.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Staff", staffId));
        if (clinician.getBaseRole() != BaseRole.CLINICAL_STAFF || clinician.getPermissionScope() != PermissionScope.NEUROLOGIST)
            throw new BadRequestException("Appointments must be assigned to a neurologist", "staffId");
        Instant start = request.scheduledTime(); Instant end = start.plusSeconds(request.durationMinutes() * 60L);
        if (appointments.staffHasConflict(clinician.getId(), start, end) || appointments.patientHasConflict(patient.getId(), start, end))
            throw new ConflictException("The patient or clinician already has an appointment at that time");
        Appointment a = new Appointment(); a.setPatient(patient); a.setStaff(clinician); a.setScheduledTime(start);
        a.setDurationMinutes(request.durationMinutes()); a.setReason(request.reason());
        return AppointmentResponse.from(appointments.save(a));
    }
    @Transactional
    public AppointmentResponse setStatus(UUID id, AppointmentStatus status) {
        Appointment a = appointments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        a.setStatus(status); return AppointmentResponse.from(a);
    }
    @Transactional(readOnly = true)
    public AppointmentResponse get(UUID id) {
        Appointment a = PatientRecordAccess.requireReadable(appointments.findById(id), x -> x.getPatient().getId(), "Appointment", id);
        return AppointmentResponse.from(a);
    }
}
