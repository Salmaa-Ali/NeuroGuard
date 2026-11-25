package com.neuroguard.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Response shapes of the dashboard endpoints (aggregates only: no patient identifiers). */
public final class DashboardDtos {

    private DashboardDtos() {
    }

    /** {@code month} as "YYYY-MM" (UTC). */
    public record MonthCount(String month, long count) {
    }

    /** ICD-10 category (first 3 characters) with its title, when the category itself is a known code. */
    public record CategoryCount(String category, String description, long count) {
    }

    public record StatusCount(String status, long count) {
    }

    /** {@code isoWeek} as "YYYY-Www" (ISO-8601, weeks start on Monday, UTC); {@code weekStart} is that Monday. */
    public record WeekCount(String isoWeek, LocalDate weekStart, long count) {
    }

    /**
     * {@code revenue} = sum of COMPLETED payments in the range (by payment date);
     * {@code unpaidCount}/{@code unpaidAmount} = UNPAID invoices in the range (by issue date).
     */
    public record FinanceSummary(BigDecimal revenue, long completedPayments, long unpaidCount, BigDecimal unpaidAmount) {
    }
}
