package com.neuroguard.security;

import com.neuroguard.audit.entity.ActorType;
import com.neuroguard.staff.entity.PermissionScope;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

/**
 * The authenticated caller, read from the validated JWT. Services use this instead of parsing tokens.
 * {@code scope} is only set for CLINICAL_STAFF.
 */
public record CurrentUser(UUID id, UserType userType, Role role, PermissionScope scope) {

    public static final String CLAIM_USER_TYPE = "user_type";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_PERM_SCOPE = "perm_scope";

    /** @throws AuthenticationCredentialsNotFoundException (401) if nobody is authenticated */
    public static CurrentUser require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken token)) {
            throw new AuthenticationCredentialsNotFoundException("No authenticated user");
        }
        return from(token.getToken());
    }

    static CurrentUser from(Jwt jwt) {
        try {
            String scope = jwt.getClaimAsString(CLAIM_PERM_SCOPE);
            return new CurrentUser(
                    UUID.fromString(jwt.getSubject()),
                    UserType.valueOf(jwt.getClaimAsString(CLAIM_USER_TYPE)),
                    Role.valueOf(jwt.getClaimAsString(CLAIM_ROLE)),
                    scope == null ? null : PermissionScope.valueOf(scope));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BadCredentialsException("Invalid token claims");
        }
    }

    public boolean isStaff() {
        return userType == UserType.STAFF;
    }

    public boolean isPatient() {
        return userType == UserType.PATIENT;
    }

    public ActorType actorType() {
        return isStaff() ? ActorType.STAFF : ActorType.PATIENT;
    }
}
