package com.neuroguard.imaging.controller;
import com.neuroguard.common.PageResponse;
import com.neuroguard.imaging.dto.*;
import com.neuroguard.imaging.entity.MriOrderStatus;
import com.neuroguard.imaging.service.MriOrderService;
import com.neuroguard.security.Authz;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController @RequestMapping("/api")
public class MriOrderController {
    private final MriOrderService service;
    public MriOrderController(MriOrderService service){this.service=service;}
    @GetMapping("/mri-orders") @PreAuthorize(Authz.MRI_ORDER_READ_ALL)
    public PageResponse<MriOrderResponse> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.list(null,page,size);}
    @GetMapping("/patients/{patientId}/mri-orders") @PreAuthorize("(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST','MRI_TECHNOLOGIST')) or @access.isSelf(#patientId)")
    public PageResponse<MriOrderResponse> listPatient(@PathVariable UUID patientId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return service.list(patientId,page,size);}
    @GetMapping("/mri-orders/{id}") @PreAuthorize("(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST','MRI_TECHNOLOGIST')) or hasRole('PATIENT')")
    public MriOrderResponse get(@PathVariable UUID id){return service.get(id);}
    @PostMapping("/mri-orders") @PreAuthorize(Authz.MRI_ORDER_WRITE)
    public MriOrderResponse create(@Valid @RequestBody CreateMriOrderRequest request){return service.create(request);}
    @PatchMapping("/mri-orders/{id}/status") @PreAuthorize(Authz.MRI_ORDER_STATUS_WRITE)
    public MriOrderResponse status(@PathVariable UUID id,@Valid @RequestBody StatusRequest request){return service.setStatus(id,request.status());}
    public record StatusRequest(@NotNull MriOrderStatus status){}
}
