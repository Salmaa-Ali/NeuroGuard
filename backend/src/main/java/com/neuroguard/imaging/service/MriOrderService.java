package com.neuroguard.imaging.service;
import com.neuroguard.appointment.repository.AppointmentRepository;
import com.neuroguard.appointment.entity.Appointment;
import com.neuroguard.common.*;
import com.neuroguard.imaging.dto.*;
import com.neuroguard.imaging.entity.*;
import com.neuroguard.imaging.repository.MriOrderRepository;
import com.neuroguard.patient.repository.PatientRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
@Service
public class MriOrderService {
    private final MriOrderRepository orders; private final AppointmentRepository appointments; private final PatientRepository patients;
    public MriOrderService(MriOrderRepository orders, AppointmentRepository appointments, PatientRepository patients) { this.orders=orders; this.appointments=appointments; this.patients=patients; }
    @Transactional(readOnly=true) public PageResponse<MriOrderResponse> list(UUID patientId,int page,int size) {
        var pageable=Pages.of(page,size, Sort.by(Sort.Order.desc("createdAt"),Sort.Order.asc("id")));
        if(patientId!=null && !patients.existsById(patientId)) throw new ResourceNotFoundException("Patient",patientId);
        return PageResponse.of(patientId==null?orders.findAllBy(pageable):orders.findByPatientId(patientId,pageable),MriOrderResponse::from);
    }
    @Transactional public MriOrderResponse create(CreateMriOrderRequest request) {
        Appointment appointment=appointments.findById(request.appointmentId()).orElseThrow(()->new ResourceNotFoundException("Appointment",request.appointmentId()));
        MriOrder order=new MriOrder(); order.setAppointment(appointment); order.setPatient(appointment.getPatient());
        order.setAccessionNumber(request.accessionNumber().trim()); order.setIndication(request.indication());
        return MriOrderResponse.from(orders.save(order));
    }
    @Transactional public MriOrderResponse setStatus(UUID id,MriOrderStatus status) {
        MriOrder order=orders.findById(id).orElseThrow(()->new ResourceNotFoundException("MRI order",id)); order.setStatus(status); return MriOrderResponse.from(order);
    }
    @Transactional(readOnly=true) public MriOrderResponse get(UUID id) {
        MriOrder order=PatientRecordAccess.requireReadable(orders.findById(id),o->o.getPatient().getId(),"MRI order",id); return MriOrderResponse.from(order);
    }
}
