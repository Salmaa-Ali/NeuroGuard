package com.neuroguard.imaging.dto;
import com.neuroguard.imaging.entity.*;
import java.util.UUID;
public record MriOrderResponse(UUID id, UUID appointmentId, UUID patientId, String patientName,
        String accessionNumber, String indication, MriOrderStatus status) {
    public static MriOrderResponse from(MriOrder o) { return new MriOrderResponse(o.getId(), o.getAppointment().getId(),
            o.getPatient().getId(), o.getPatient().getName(), o.getAccessionNumber(), o.getIndication(), o.getStatus()); }
}
