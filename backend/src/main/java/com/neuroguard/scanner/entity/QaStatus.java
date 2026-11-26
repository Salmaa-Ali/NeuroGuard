package com.neuroguard.scanner.entity;

/** Derived per scanner, never stored. A signal for review only: it never blocks or changes patient studies. */
public enum QaStatus {
    NEVER_CALIBRATED,
    FAILED,
    OVERDUE,
    DUE_SOON,
    OK
}