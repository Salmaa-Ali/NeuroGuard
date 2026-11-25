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

    /** Diagnoses, treatment plans, prescriptions, write: Neurologist only. */
    public static final String CLINICAL_WRITE =
            "hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST')";

    /** Clinical records of one patient (path variable patientId): Neurologist, Radiologist, or the patient (own). */
    public static final String CLINICAL_READ_BY_PATIENT =
            "(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST'))"
                    + " or @access.isSelf(#patientId)";

    /**
     * One clinical record loaded by its own id: Neurologist, Radiologist, or a patient. The patient part is
     * only a gate: the service must check the record belongs to them (PatientRecordAccess).
     */
    public static final String CLINICAL_READ_BY_RECORD =
            "(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST'))"
                    + " or hasRole('PATIENT')";

    /** Patient profile & history, read: Neurologist, Radiologist, MRI tech, Billing clerk, or the patient (own). */
    public static final String PATIENT_PROFILE_READ =
            "(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST','MRI_TECHNOLOGIST','BILLING_CLERK'))"
                    + " or @access.isSelf(#patientId)";
}
