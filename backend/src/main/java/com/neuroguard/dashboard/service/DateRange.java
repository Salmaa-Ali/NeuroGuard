package com.neuroguard.dashboard.service;

import com.neuroguard.common.BadRequestException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Optional, inclusive date range in UTC. Either end may be open. Produces SQL conditions so filtering
 * happens in the database, and only adds a condition for the ends that are set.
 */
public record DateRange(LocalDate from, LocalDate to) {

    public DateRange {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be on or before to", "from");
        }
    }

    /** For TIMESTAMPTZ columns: [from 00:00Z, to+1 00:00Z). */
    public String onTimestamp(String column, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder();
        if (from != null) {
            sql.append(" AND ").append(column).append(" >= :rangeFrom");
            params.addValue("rangeFrom", OffsetDateTime.of(from.atStartOfDay(), ZoneOffset.UTC));
        }
        if (to != null) {
            sql.append(" AND ").append(column).append(" < :rangeToExclusive");
            params.addValue("rangeToExclusive", OffsetDateTime.of(to.plusDays(1).atStartOfDay(), ZoneOffset.UTC));
        }
        return sql.toString();
    }

    /** For DATE columns: from <= column <= to. */
    public String onDate(String column, MapSqlParameterSource params) {
        StringBuilder sql = new StringBuilder();
        if (from != null) {
            sql.append(" AND ").append(column).append(" >= :rangeFromDate");
            params.addValue("rangeFromDate", from);
        }
        if (to != null) {
            sql.append(" AND ").append(column).append(" <= :rangeToDate");
            params.addValue("rangeToDate", to);
        }
        return sql.toString();
    }
}
