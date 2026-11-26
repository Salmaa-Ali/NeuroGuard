package com.neuroguard.imaging.service;
import com.neuroguard.appointment.repository.AppointmentRepository;
import com.neuroguard.appointment.entity.Appointment;
import com.neuroguard.appointment.entity.AppointmentStatus;
import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.common.*;
import com.neuroguard.imaging.dto.*;
import com.neuroguard.imaging.entity.*;
import com.neuroguard.imaging.repository.MriOrderRepository;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.CurrentUser;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class MriOrderService {
    static final String ENTITY = "MRI_ORDER";
    private final MriOrderRepository orders; private final AppointmentRepository appointments; private final PatientRepository patients;
    private final AccessionNumberGenerator accessionNumbers; private final AuditService auditService;
    public MriOrderService(MriOrderRepository orders, AppointmentRepository appointments, PatientRepository patients,
                           AccessionNumberGenerator accessionNumbers, AuditService auditService) {
        this.orders=orders; this.appointments=appointments; this.patients=patients; this.accessionNumbers=accessionNumbers; this.auditService=auditService;
    }
    @Transactional(readOnly=true) public PageResponse<MriOrderResponse> list(UUID patientId,int page,int size) {
        var pageable=Pages.of(page,size, Sort.by(Sort.Order.desc("createdAt"),Sort.Order.asc("id")));
        if(patientId!=null && !patients.existsById(patientId)) throw new ResourceNotFoundException("Patient",patientId);
        return PageResponse.of(patientId==null?orders.findAllBy(pageable):orders.findByPatientId(patientId,pageable),MriOrderResponse::from);
    }
    /**
     * The patient always comes from the appointment and the accession number from the sequence, so the client
     * chooses neither. CANCELLED and MISSED appointments are refused (409): no visit took place. The number is drawn only after the appointment checks pass, to keep gaps rare.
     */
    @Transactional public MriOrderResponse create(CreateMriOrderRequest request) {
        Appointment appointment=appointments.findById(request.appointmentId()).orElseThrow(()->new ResourceNotFoundException("Appointment",request.appointmentId()));
        if(appointment.getStatus()==AppointmentStatus.CANCELLED||appointment.getStatus()==AppointmentStatus.MISSED)
            throw new ConflictException("MRI orders cannot be placed on a "+appointment.getStatus().name().toLowerCase()+" appointment");
        MriOrder order=new MriOrder(); order.setAppointment(appointment); order.setPatient(appointment.getPatient());
        order.setAccessionNumber(accessionNumbers.next()); order.setIndication(request.indication());
        order=orders.saveAndFlush(order);
        audit(AuditAction.MRI_ORDER_CREATED, order.getId());
        return MriOrderResponse.from(order);
    }
    @Transactional public MriOrderResponse setStatus(UUID id,MriOrderStatus status) {
        MriOrder order=orders.findById(id).orElseThrow(()->new ResourceNotFoundException("MRI order",id)); order.setStatus(status);
        orders.saveAndFlush(order);
        audit(AuditAction.MRI_ORDER_STATUS_CHANGED, id);
        return MriOrderResponse.from(order);
    }
    @Transactional(readOnly=true) public MriOrderResponse get(UUID id) {
        MriOrder order=PatientRecordAccess.requireReadable(orders.findById(id),o->o.getPatient().getId(),"MRI order",id); return MriOrderResponse.from(order);
    }
    private void audit(String action, UUID orderId) {
        CurrentUser actor=CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), action, ENTITY, orderId);
    }
}
