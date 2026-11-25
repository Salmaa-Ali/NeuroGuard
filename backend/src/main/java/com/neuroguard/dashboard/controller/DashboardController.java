package com.neuroguard.dashboard.controller;

import com.neuroguard.dashboard.dto.DashboardDtos.CategoryCount;
import com.neuroguard.dashboard.dto.DashboardDtos.FinanceSummary;
import com.neuroguard.dashboard.dto.DashboardDtos.MonthCount;
import com.neuroguard.dashboard.dto.DashboardDtos.StatusCount;
import com.neuroguard.dashboard.dto.DashboardDtos.WeekCount;
import com.neuroguard.dashboard.service.DashboardService;
import com.neuroguard.dashboard.service.DateRange;
import com.neuroguard.security.Authz;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * One endpoint per section so each has its own rule (see docs/permissions.md, "proposed").
 * All take optional {@code from}/{@code to} (ISO dates, inclusive, UTC).
 * HL7 and scanner QA sections: TODO for their owners, see DashboardService.
 */
@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/patients/new-per-month")
    @PreAuthorize(Authz.DASHBOARD_CLINICAL)
    public List<MonthCount> newPatientsPerMonth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.newPatientsPerMonth(new DateRange(from, to));
    }

    @GetMapping("/diagnoses/by-category")
    @PreAuthorize(Authz.DASHBOARD_CLINICAL)
    public List<CategoryCount> diagnosesByCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.diagnosesByCategory(new DateRange(from, to));
    }

    @GetMapping("/appointments/by-status")
    @PreAuthorize(Authz.DASHBOARD_CLINICAL)
    public List<StatusCount> appointmentsByStatus(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.appointmentsByStatus(new DateRange(from, to));
    }

    @GetMapping("/mri-orders/per-week")
    @PreAuthorize(Authz.DASHBOARD_CLINICAL)
    public List<WeekCount> mriOrdersPerWeek(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.mriOrdersPerWeek(new DateRange(from, to));
    }

    @GetMapping("/finance")
    @PreAuthorize(Authz.DASHBOARD_FINANCE)
    public FinanceSummary finance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.finance(new DateRange(from, to));
    }
}
