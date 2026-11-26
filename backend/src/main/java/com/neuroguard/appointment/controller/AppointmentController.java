package com.neuroguard.appointment.controller;

import com.neuroguard.appointment.dto.*;
import com.neuroguard.appointment.entity.AppointmentStatus;
import com.neuroguard.appointment.service.AppointmentService;
import com.neuroguard.common.PageResponse;
import com.neuroguard.security.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api")
public class AppointmentController {
    private final AppointmentService service;
    public AppointmentController(AppointmentService service) { this.service = service; }
    @GetMapping("/appointments") @PreAuthorize(Authz.APPOINTMENT_READ_ALL)
    public PageResponse<AppointmentResponse> list(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) { return service.list(null,page,size); }
    @GetMapping("/patients/{patientId}/appointments") @PreAuthorize("(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST','MRI_TECHNOLOGIST','BILLING_CLERK')) or @access.isSelf(#patientId)")
    public PageResponse<AppointmentResponse> listPatient(@PathVariable UUID patientId, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="50") int size) { return service.list(patientId,page,size); }
    @GetMapping("/appointments/{id}") @PreAuthorize("(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST','MRI_TECHNOLOGIST','BILLING_CLERK')) or hasRole('PATIENT')")
    public AppointmentResponse get(@PathVariable UUID id) { return service.get(id); }
    @PostMapping("/appointments") @PreAuthorize(Authz.APPOINTMENT_WRITE)
    public AppointmentResponse create(@Valid @RequestBody CreateAppointmentRequest request) { return service.create(request); }
    @PatchMapping("/appointments/{id}/status") @PreAuthorize(Authz.APPOINTMENT_WRITE)
    public AppointmentResponse status(@PathVariable UUID id, @RequestBody StatusRequest request) { return service.setStatus(id, request.status()); }
    public record StatusRequest(@jakarta.validation.constraints.NotNull AppointmentStatus status) { }
}
