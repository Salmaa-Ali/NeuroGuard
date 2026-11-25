package com.neuroguard.common;

import com.neuroguard.security.CurrentUser;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * "Own" check for records loaded by their own id (a diagnosis, plan, invoice, ...), where
 * {@code @access.isSelf(#patientId)} cannot help because the URL carries no patient id.
 */
public final class PatientRecordAccess {

    private PatientRecordAccess() {
    }

    /**
     * Staff (already filtered by @PreAuthorize) get the record or a 404. Patients get the record only if it
     * is theirs; for someone else's record AND for an unknown id they get 403, so they cannot probe which
     * ids exist (same behaviour as GET /api/patients/{patientId}).
     */
    public static <T> T requireReadable(Optional<T> record, Function<T, UUID> patientIdOf,
                                        String resource, Object id) {
        CurrentUser user = CurrentUser.require();
        if (record.isEmpty()) {
            if (user.isPatient()) {
                throw new AccessDeniedException("Not your record");
            }
            throw new ResourceNotFoundException(resource, id);
        }
        T value = record.get();
        if (user.isPatient() && !user.id().equals(patientIdOf.apply(value))) {
            throw new AccessDeniedException("Not your record");
        }
        return value;
    }
}
