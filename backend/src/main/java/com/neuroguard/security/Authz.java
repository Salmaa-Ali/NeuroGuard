package com.neuroguard.security;

/**
 * Shared @PreAuthorize expressions. One place so the permission matrix (docs/permissions.md)
 * is not copy-pasted across controllers. Add new expressions here as modules are built.
 */
public final class Authz {

    private Authz() {
    }

    /** Staff accounts: Admin RW. */
    public static final String ADMIN_ONLY = "hasRole('ADMIN')";

    /** Any staff member (Admin or any clinical scope), never patients. Used for reference data such as ICD-10. */
    public static final String ANY_STAFF = "hasAnyRole('ADMIN','CLINICAL_STAFF')";

    /** Patient profile & history, read: Neurologist, Radiologist, MRI tech, Billing clerk, or the patient (own). */
    public static final String PATIENT_PROFILE_READ =
            "(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST','MRI_TECHNOLOGIST','BILLING_CLERK'))"
                    + " or @access.isSelf(#patientId)";
}
