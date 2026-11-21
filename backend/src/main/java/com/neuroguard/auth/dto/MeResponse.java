package com.neuroguard.auth.dto;

import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.staff.entity.PermissionScope;

import java.util.UUID;

/** The authenticated caller. role/scope are the ones in the token (what is actually in effect). */
public record MeResponse(
        UUID id,
        String name,
        String email,
        UserType userType,
        Role role,
        PermissionScope scope) {
}
