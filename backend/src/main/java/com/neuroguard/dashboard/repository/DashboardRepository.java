package com.neuroguard.dashboard.repository;

import com.neuroguard.dashboard.dto.DashboardDtos.CategoryCount;
import com.neuroguard.dashboard.dto.DashboardDtos.MonthCount;
import com.neuroguard.dashboard.dto.DashboardDtos.StatusCount;
import com.neuroguard.dashboard.service.DateRange;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Aggregate SQL only: every figure is computed by GROUP BY / SUM in Postgres, so no entities are loaded.
 * Reads tables owned by other modules (patient, appointment, mri_order) read-only.
 * Grouping uses UTC explicitly ({@code AT TIME ZONE 'UTC'}), independent of the DB session time zone.
 */
@Repository
public class DashboardRepository {

    /** A count and a money sum. */
    public record Totals(long count, BigDecimal sum) {
    }

    /** Monday of an ISO week and the number of rows in it. */
    public record WeekRow(LocalDate weekStart, long count) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public DashboardRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MonthCount> newPatientsPerMonth(DateRange range) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT to_char(date_trunc('month', created_at AT TIME ZONE 'UTC'), 'YYYY-MM') AS month,"
                + " count(*) AS n FROM patient WHERE 1 = 1"
                + range.onTimestamp("created_at", p)
                + " GROUP BY 1 ORDER BY 1";
        return jdbc.query(sql, p, (rs, i) -> new MonthCount(rs.getString("month"), rs.getLong("n")));
    }

    public List<CategoryCount> diagnosesByCategory(DateRange range) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT c.category, cat.description, count(*) AS n"
                + " FROM diagnosis d"
                + " JOIN icd10_code c ON c.code = d.icd10_code"
                + " LEFT JOIN icd10_code cat ON cat.code = c.category"
                + " WHERE 1 = 1"
                + range.onTimestamp("d.diagnosed_at", p)
                + " GROUP BY c.category, cat.description ORDER BY n DESC, c.category";
        return jdbc.query(sql, p, (rs, i) ->
                new CategoryCount(rs.getString("category"), rs.getString("description"), rs.getLong("n")));
    }

    public List<StatusCount> appointmentsByStatus(DateRange range) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT status, count(*) AS n FROM appointment WHERE 1 = 1"
                + range.onTimestamp("scheduled_time", p)
                + " GROUP BY status";
        return jdbc.query(sql, p, (rs, i) -> new StatusCount(rs.getString("status"), rs.getLong("n")));
    }

    /** date_trunc('week') is ISO: weeks start on Monday. */
    public List<WeekRow> mriOrdersPerWeek(DateRange range) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT date_trunc('week', created_at AT TIME ZONE 'UTC')::date AS week_start, count(*) AS n"
                + " FROM mri_order WHERE 1 = 1"
                + range.onTimestamp("created_at", p)
                + " GROUP BY 1 ORDER BY 1";
        return jdbc.query(sql, p, (rs, i) -> new WeekRow(rs.getObject("week_start", LocalDate.class), rs.getLong("n")));
    }

    /** COMPLETED payments by payment date. */
    public Totals completedPayments(DateRange range) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT count(*) AS n, COALESCE(sum(amount_paid), 0) AS total FROM payment"
                + " WHERE status = 'COMPLETED'"
                + range.onTimestamp("payment_date", p);
        return jdbc.queryForObject(sql, p, (rs, i) -> new Totals(rs.getLong("n"), rs.getBigDecimal("total")));
    }

    /** UNPAID invoices by issue date. */
    public Totals unpaidInvoices(DateRange range) {
        MapSqlParameterSource p = new MapSqlParameterSource();
        String sql = "SELECT count(*) AS n, COALESCE(sum(total_amount), 0) AS total FROM invoice"
                + " WHERE status = 'UNPAID'"
                + range.onDate("issue_date", p);
        return jdbc.queryForObject(sql, p, (rs, i) -> new Totals(rs.getLong("n"), rs.getBigDecimal("total")));
    }
}
