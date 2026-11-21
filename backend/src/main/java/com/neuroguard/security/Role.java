package com.neuroguard.security;

/** Value of the "role" JWT claim; becomes the authority ROLE_&lt;name&gt;. */
public enum Role {
    ADMIN,
    CLINICAL_STAFF,
    PATIENT
}
