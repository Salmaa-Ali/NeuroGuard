package com.neuroguard.staff.dto;

import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import jakarta.validation.constraints.NotNull;

public record ChangeStaffRoleRequest(
        @NotNull(message = "is required") BaseRole baseRole,
        PermissionScope permissionScope) {
}
