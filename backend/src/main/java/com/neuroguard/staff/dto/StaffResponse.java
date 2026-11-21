package com.neuroguard.staff.dto;

import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import com.neuroguard.staff.entity.Staff;

import java.time.Instant;
import java.util.UUID;

/** Never includes the password hash. */
public record StaffResponse(
        UUID id,
        String name,
        String email,
        BaseRole baseRole,
        PermissionScope permissionScope,
        Instant createdAt,
        Instant updatedAt) {

    public static StaffResponse from(Staff s) {
        return new StaffResponse(s.getId(), s.getName(), s.getEmail(), s.getBaseRole(),
                s.getPermissionScope(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
