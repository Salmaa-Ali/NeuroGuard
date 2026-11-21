package com.neuroguard.security;

import org.springframework.stereotype.Component;

import java.util.UUID;

/** Reusable checks for @PreAuthorize, referenced as {@code @access.isSelf(#patientId)}. */
@Component("access")
public class AccessChecks {

    /** True if the caller is a patient and patientId is their own id. */
    public boolean isSelf(UUID patientId) {
        CurrentUser user = CurrentUser.require();
        return patientId != null && user.isPatient() && user.id().equals(patientId);
    }

    /** True if the caller is CLINICAL_STAFF holding one of the given permission scopes. */
    public boolean hasAnyScope(String... scopes) {
        CurrentUser user = CurrentUser.require();
        if (user.role() != Role.CLINICAL_STAFF || user.scope() == null) {
            return false;
        }
        for (String scope : scopes) {
            if (user.scope().name().equals(scope)) {
                return true;
            }
        }
        return false;
    }
}
