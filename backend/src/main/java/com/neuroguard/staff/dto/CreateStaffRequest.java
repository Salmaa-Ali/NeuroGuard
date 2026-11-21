package com.neuroguard.staff.dto;

import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank(message = "must not be blank") @Size(max = 255) String name,
        @NotBlank(message = "must not be blank") @Email(message = "must be a valid email") @Size(max = 255) String email,
        @NotBlank(message = "must not be blank")
        @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password,
        @NotNull(message = "is required") BaseRole baseRole,
        PermissionScope permissionScope) {

    @Override
    public String toString() {
        return "CreateStaffRequest[email=" + email + ", baseRole=" + baseRole + "]";
    }
}
