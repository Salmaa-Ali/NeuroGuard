package com.neuroguard.scanner.service;

import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.scanner.dto.AccessoryResponse;
import com.neuroguard.scanner.dto.CalibrationSummary;
import com.neuroguard.scanner.dto.QaOverview;
import com.neuroguard.scanner.dto.QaSummary;
import com.neuroguard.scanner.dto.ScannerDetail;
import com.neuroguard.scanner.dto.ScannerListItem;
import com.neuroguard.scanner.entity.CalibrationSession;
import com.neuroguard.scanner.entity.QaStatus;
import com.neuroguard.scanner.entity.ScannerDevice;
import com.neuroguard.scanner.entity.ScannerStatus;
import com.neuroguard.scanner.repository.CalibrationSessionRepository;
import com.neuroguard.scanner.repository.ScannerAccessoryRepository;
import com.neuroguard.scanner.repository.ScannerDeviceRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read side: QA status is derived on every read from the latest session and today's date (never stored).
 * The scanner list is filtered in memory because qaStatus is derived; scanner counts are small.
 */
@Service
public class ScannerQueryService {

    private final ScannerDeviceRepository scanners;
    private final ScannerAccessoryRepository accessories;
    private final CalibrationSessionRepository sessions;
    private final ScannerService scannerService;
    private final ScannerQaProperties properties;
    private final Clock clock;

    public ScannerQueryService(ScannerDeviceRepository scanners, ScannerAccessoryRepository accessories,
                               CalibrationSessionRepository sessions, ScannerService scannerService,
                               ScannerQaProperties properties, Clock clock) {
        this.scanners = scanners;
        this.accessories = accessories;
        this.sessions = sessions;
        this.scannerService = scannerService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public PageResponse<ScannerListItem> list(ScannerStatus status, QaStatus qaStatus, int page, int size) {
        Map<UUID, CalibrationSession> latest = latestPerScanner();
        LocalDate today = today();
        List<ScannerListItem> rows = scanners.findAll(Sort.by("name").and(Sort.by("id"))).stream()
                .filter(s -> status == null || s.getStatus() == status)
                .map(s -> listItem(s, latest.get(s.getId()), today))
                .filter(r -> qaStatus == null || r.qaStatus() == qaStatus)
                .toList();
        PageRequest p = Pages.of(page, size, Sort.unsorted());
        int from = (int) Math.min(p.getOffset(), rows.size());
        int to = Math.min(from + p.getPageSize(), rows.size());
        int totalPages = (int) Math.ceil((double) rows.size() / p.getPageSize());
        return new PageResponse<>(rows.subList(from, to), p.getPageNumber(), p.getPageSize(), rows.size(), totalPages);
    }

    @Transactional(readOnly = true)
    public ScannerDetail detail(UUID id) {
        ScannerDevice s = scannerService.find(id);
        CalibrationSession last = sessions.findFirstByScannerIdOrderByPerformedAtDescIdDesc(id).orElse(null);
        List<AccessoryResponse> coils = accessories.findByScannerIdOrderByNameAscIdAsc(id).stream()
                .map(AccessoryResponse::from).toList();
        return new ScannerDetail(s.getId(), s.getName(), s.getManufacturer(), s.getModel(), s.getSerialNumber(),
                s.getStationName(), s.getFieldStrength(), s.getStatus(), s.getLocation(), s.getInstallDate(),
                s.getCalibrationIntervalDays(), qaStatus(last, today()),
                last == null ? null : CalibrationSummary.from(last), coils, s.getCreatedAt(), s.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public QaSummary qaSummary(UUID id) {
        ScannerDevice s = scannerService.find(id);
        CalibrationSession last = sessions.findFirstByScannerIdOrderByPerformedAtDescIdDesc(id).orElse(null);
        LocalDate today = today();
        QaStatus status = qaStatus(last, today);
        return new QaSummary(s.getId(), s.getName(), s.getFieldStrength(), status,
                status == QaStatus.FAILED || status == QaStatus.OVERDUE,
                last == null ? null : last.getPerformedAt(),
                last == null ? null : last.getResult(),
                last == null ? null : last.getNextDueDate(),
                last == null ? null : last.getRiskScore(),
                last == null ? null : last.getId(),
                last == null ? null : last.getCreatedAt(),
                today);
    }

    /** RETIRED scanners are left out: they are no longer in use. */
    @Transactional(readOnly = true)
    public QaOverview overview() {
        Map<UUID, CalibrationSession> latest = latestPerScanner();
        LocalDate today = today();
        Map<QaStatus, Long> counts = scanners.findAll().stream()
                .filter(s -> s.getStatus() != ScannerStatus.RETIRED)
                .collect(Collectors.groupingBy(s -> qaStatus(latest.get(s.getId()), today), Collectors.counting()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        return new QaOverview(total, counts.getOrDefault(QaStatus.OK, 0L), counts.getOrDefault(QaStatus.DUE_SOON, 0L),
                counts.getOrDefault(QaStatus.OVERDUE, 0L), counts.getOrDefault(QaStatus.FAILED, 0L),
                counts.getOrDefault(QaStatus.NEVER_CALIBRATED, 0L));
    }

    private ScannerListItem listItem(ScannerDevice s, CalibrationSession last, LocalDate today) {
        return new ScannerListItem(s.getId(), s.getName(), s.getManufacturer(), s.getModel(), s.getFieldStrength(),
                s.getStationName(), s.getStatus(), qaStatus(last, today),
                last == null ? null : last.getPerformedAt(),
                last == null ? null : last.getResult(),
                last == null ? null : last.getNextDueDate(),
                last == null ? null : last.getRiskScore());
    }

    private QaStatus qaStatus(CalibrationSession last, LocalDate today) {
        return last == null
                ? QaStatus.NEVER_CALIBRATED
                : QaStatusCalculator.status(last.getResult(), last.getNextDueDate(), today, properties.dueSoonDays());
    }

    /** Ties on performed_at (unlikely) are broken by id so the choice is stable. */
    private Map<UUID, CalibrationSession> latestPerScanner() {
        return sessions.findLatestPerScanner().stream().collect(Collectors.toMap(
                s -> s.getScanner().getId(), Function.identity(),
                (a, b) -> Comparator.comparing(CalibrationSession::getId).compare(a, b) >= 0 ? a : b));
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(ZoneOffset.UTC));
    }
}
