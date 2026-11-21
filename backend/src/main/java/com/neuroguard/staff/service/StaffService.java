package com.neuroguard.staff.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.common.BadRequestException;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.Emails;
import com.neuroguard.common.PageResponse;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.security.CurrentUser;
import com.neuroguard.staff.dto.ChangeStaffRoleRequest;
import com.neuroguard.staff.dto.CreateStaffRequest;
import com.neuroguard.staff.dto.StaffResponse;
import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class StaffService {

    private static final Set<String> SORTABLE = Set.of("name", "email", "createdAt");
    private static final int MAX_PAGE_SIZE = 100;

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public StaffService(StaffRepository staffRepository, PasswordEncoder passwordEncoder,
                        AuditService auditService) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional
    public StaffResponse create(CreateStaffRequest request) {
        validateRoleAndScope(request.baseRole(), request.permissionScope());
        String email = Emails.normalize(request.email());
        if (staffRepository.existsByEmail(email)) {
            throw new ConflictException("A staff account with this email already exists");
        }
        Staff staff = new Staff();
        staff.setName(request.name().trim());
        staff.setEmail(email);
        staff.setPasswordHash(passwordEncoder.encode(request.password()));
        staff.setBaseRole(request.baseRole());
        staff.setPermissionScope(request.permissionScope());
        staff = staffRepository.saveAndFlush(staff);

        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.STAFF_CREATED, "STAFF", staff.getId());
        return StaffResponse.from(staff);
    }

    @Transactional(readOnly = true)
    public PageResponse<StaffResponse> list(int page, int size, String sortBy) {
        if (!SORTABLE.contains(sortBy)) {
            throw new BadRequestException("sortBy must be one of " + SORTABLE, "sortBy");
        }
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(sortBy).and(Sort.by("id")));
        Page<Staff> result = staffRepository.findAll(pageable);
        return PageResponse.of(result, StaffResponse::from);
    }

    @Transactional(readOnly = true)
    public StaffResponse get(UUID id) {
        return StaffResponse.from(find(id));
    }

    @Transactional
    public StaffResponse changeRole(UUID id, ChangeStaffRoleRequest request) {
        validateRoleAndScope(request.baseRole(), request.permissionScope());
        Staff staff = find(id);
        boolean demotingAdmin = staff.getBaseRole() == BaseRole.ADMIN && request.baseRole() != BaseRole.ADMIN;
        if (demotingAdmin && staffRepository.countByBaseRole(BaseRole.ADMIN) <= 1) {
            throw new ConflictException("Cannot demote the last remaining ADMIN");
        }
        staff.setBaseRole(request.baseRole());
        staff.setPermissionScope(request.permissionScope());
        staff = staffRepository.saveAndFlush(staff);

        CurrentUser actor = CurrentUser.require();
        auditService.record(actor.actorType(), actor.id(), AuditAction.STAFF_ROLE_CHANGED, "STAFF", staff.getId());
        return StaffResponse.from(staff);
    }

    private Staff find(UUID id) {
        return staffRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Staff", id));
    }

    /** CLINICAL_STAFF needs a permission scope; ADMIN must not have one. */
    private void validateRoleAndScope(BaseRole role, PermissionScope scope) {
        if (role == BaseRole.CLINICAL_STAFF && scope == null) {
            throw new BadRequestException("permissionScope is required for CLINICAL_STAFF", "permissionScope");
        }
        if (role == BaseRole.ADMIN && scope != null) {
            throw new BadRequestException("permissionScope must be empty for ADMIN", "permissionScope");
        }
    }
}
