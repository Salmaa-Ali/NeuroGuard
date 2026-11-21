package com.neuroguard.auth.dto;

import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.staff.entity.PermissionScope;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserType userType,
        Role role,
        PermissionScope scope) {

    public static LoginResponse bearer(String token, long expiresInSeconds, UserType userType,
                                       Role role, PermissionScope scope) {
        return new LoginResponse(token, "Bearer", expiresInSeconds, userType, role, scope);
    }
}
