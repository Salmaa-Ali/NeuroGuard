package com.neuroguard.staff.controller;

import com.neuroguard.common.PageResponse;
import com.neuroguard.security.Authz;
import com.neuroguard.staff.dto.ChangeStaffRoleRequest;
import com.neuroguard.staff.dto.CreateStaffRequest;
import com.neuroguard.staff.dto.StaffResponse;
import com.neuroguard.staff.service.StaffService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize(Authz.ADMIN_ONLY)
@Tag(name = "Admin - Staff")
public class AdminStaffController {

    private final StaffService staffService;

    public AdminStaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping
    public ResponseEntity<StaffResponse> create(@Valid @RequestBody CreateStaffRequest request) {
        StaffResponse created = staffService.create(request);
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @GetMapping
    public PageResponse<StaffResponse> list(@RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(defaultValue = "name") String sortBy) {
        return staffService.list(page, size, sortBy);
    }

    @GetMapping("/{id}")
    public StaffResponse get(@PathVariable UUID id) {
        return staffService.get(id);
    }

    @PatchMapping("/{id}/role")
    public StaffResponse changeRole(@PathVariable UUID id, @Valid @RequestBody ChangeStaffRoleRequest request) {
        return staffService.changeRole(id, request);
    }
}
