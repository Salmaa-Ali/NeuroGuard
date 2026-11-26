package com.neuroguard.scanner.dto;

/** Counts per QA status for the dashboard. RETIRED scanners are not counted. */
public record QaOverview(long total, long ok, long dueSoon, long overdue, long failed, long neverCalibrated) {
}
