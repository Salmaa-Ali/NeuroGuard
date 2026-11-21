package com.neuroguard.audit;

/** Values for audit_log.action. */
public final class AuditAction {

    private AuditAction() {
    }

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String STAFF_CREATED = "STAFF_CREATED";
    public static final String STAFF_ROLE_CHANGED = "STAFF_ROLE_CHANGED";
}
