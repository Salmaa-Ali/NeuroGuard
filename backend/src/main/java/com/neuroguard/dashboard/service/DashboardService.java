package com.neuroguard.dashboard.service;

import com.neuroguard.appointment.entity.AppointmentStatus;
import com.neuroguard.billing.service.Money;
import com.neuroguard.dashboard.dto.DashboardDtos.CategoryCount;
import com.neuroguard.dashboard.dto.DashboardDtos.FinanceSummary;
import com.neuroguard.dashboard.dto.DashboardDtos.MonthCount;
import com.neuroguard.dashboard.dto.DashboardDtos.StatusCount;
import com.neuroguard.dashboard.dto.DashboardDtos.WeekCount;
import com.neuroguard.dashboard.repository.DashboardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Read-only analytics. Every section takes the same optional inclusive date range (UTC), applied to the
 * date that defines the event: patient created_at, diagnosis diagnosed_at, appointment scheduled_time,
 * MRI order created_at, payment payment_date, invoice issue_date.
 *
 * <p>TODO(HL7 owner): HL7 message metrics (messages received/sent, failures) belong to the HL7 module;
 * add them as a separate section/endpoint here once that module exists. Not implemented on purpose.
 * <p>TODO(scanner owner): scanner QA metrics (calibrations due/overdue, risk scores) belong to the
 * scanner/calibration module; add them here once it exposes them. Not implemented on purpose.
 */
@Service
public class DashboardService {

    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    /** Only months with registrations are returned, so an empty range gives an empty list rather than zeros. */
    @Transactional(readOnly = true)
    public List<MonthCount> newPatientsPerMonth(DateRange range) {
        return repository.newPatientsPerMonth(range);
    }

    /**
     * Grouped by the 3-character ICD-10 category so detailed codes (G40909) roll up with their category (G40);
     * counts only, so it is safe for roles without access to individual diagnoses.
     */
    @Transactional(readOnly = true)
    public List<CategoryCount> diagnosesByCategory(DateRange range) {
        return repository.diagnosesByCategory(range);
    }

    /**
     * Every status is listed, with 0 where there are none, so charts keep a stable set of bars and a
     * missing status cannot be mistaken for missing data.
     */
    @Transactional(readOnly = true)
    public List<StatusCount> appointmentsByStatus(DateRange range) {
        Map<String, Long> counts = repository.appointmentsByStatus(range).stream()
                .collect(Collectors.toMap(StatusCount::status, StatusCount::count));
        return Arrays.stream(AppointmentStatus.values())
                .map(s -> new StatusCount(s.name(), counts.getOrDefault(s.name(), 0L)))
                .toList();
    }

    /** Only weeks with orders are returned; labels use ISO week-based years (e.g. 2026-W01 can start in 2025). */
    @Transactional(readOnly = true)
    public List<WeekCount> mriOrdersPerWeek(DateRange range) {
        return repository.mriOrdersPerWeek(range).stream()
                .map(w -> new WeekCount(String.format(Locale.ROOT, "%d-W%02d", w.weekStart().get(IsoFields.WEEK_BASED_YEAR),
                        w.weekStart().get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)), w.weekStart(), w.count()))
                .toList();
    }

    /**
     * Revenue counts only COMPLETED payments (cash actually received), not invoiced amounts; unpaid is
     * reported separately so the two are never mixed. Cancelled invoices appear in neither.
     */
    @Transactional(readOnly = true)
    public FinanceSummary finance(DateRange range) {
        DashboardRepository.Totals paid = repository.completedPayments(range);
        DashboardRepository.Totals unpaid = repository.unpaidInvoices(range);
        return new FinanceSummary(Money.of(paid.sum()), paid.count(), unpaid.count(), Money.of(unpaid.sum()));
    }
}
