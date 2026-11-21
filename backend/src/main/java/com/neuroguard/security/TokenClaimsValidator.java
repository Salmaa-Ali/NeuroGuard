package com.neuroguard.security;

import com.neuroguard.staff.entity.PermissionScope;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Rejects (401) signed tokens whose NeuroGuard claims are missing, unknown or inconsistent. */
public class TokenClaimsValidator implements OAuth2TokenValidator<Jwt> {

    private static OAuth2TokenValidatorResult invalid(String why) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", why, null));
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            UUID.fromString(jwt.getSubject());
            UserType type = UserType.valueOf(jwt.getClaimAsString(CurrentUser.CLAIM_USER_TYPE));
            Role role = Role.valueOf(jwt.getClaimAsString(CurrentUser.CLAIM_ROLE));
            String scope = jwt.getClaimAsString(CurrentUser.CLAIM_PERM_SCOPE);
            if (scope != null) {
                PermissionScope.valueOf(scope);
            }
            boolean patientOk = type == UserType.PATIENT && role == Role.PATIENT && scope == null;
            boolean adminOk = type == UserType.STAFF && role == Role.ADMIN && scope == null;
            boolean clinicalOk = type == UserType.STAFF && role == Role.CLINICAL_STAFF && scope != null;
            return (patientOk || adminOk || clinicalOk)
                    ? OAuth2TokenValidatorResult.success()
                    : invalid("Inconsistent user_type/role/perm_scope claims");
        } catch (IllegalArgumentException | NullPointerException e) {
            return invalid("Missing or invalid NeuroGuard claims");
        }
    }
}
