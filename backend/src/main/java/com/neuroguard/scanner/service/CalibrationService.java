package com.neuroguard.scanner.service;

import com.neuroguard.audit.AuditAction;
import com.neuroguard.audit.service.AuditService;
import com.neuroguard.common.BadRequestException;
import com.neuroguard.common.ConflictException;
import com.neuroguard.common.PageResponse;
import com.neuroguard.common.Pages;
import com.neuroguard.common.ResourceNotFoundException;
import com.neuroguard.scanner.dto.CalibrationDetail;
import com.neuroguard.scanner.dto.CalibrationSummary;
import com.neuroguard.scanner.dto.MeasurementResponse;
import com.neuroguard.scanner.dto.RecordCalibrationRequest;
import com.neuroguard.scanner.entity.CalibrationMeasurement;
import com.neuroguard.scanner.entity.CalibrationResult;
import com.neuroguard.scanner.entity.CalibrationSession;
import com.neuroguard.scanner.entity.QaTest;
import com.neuroguard.scanner.entity.ScannerDevice;
import com.neuroguard.scanner.entity.ScannerStatus;
import com.neuroguard.scanner.repository.CalibrationMeasurementRepository;
import com.neuroguard.scanner.repository.CalibrationSessionRepository;
import com.neuroguard.scanner.repository.QaTestRepository;
import com.neuroguard.security.CurrentUser;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Records QA sessions. The server decides the result (PASS only if every measurement is within tolerance),
 * the next due date (performed date + the scanner's interval) and the indicative risk score.
 * A FAIL is a signal for review only: it never blocks, invalidates or changes patient studies or reports.
 */
@Service
public class CalibrationService {

    /** The risk score looks at this many sessions, the new one included. */
    static final int RISK_WINDOW = 3;

    private final ScannerService scannerService;
    private final CalibrationSessionRepository sessions;
    private final CalibrationMeasurementRepository measurements;
    private final QaTestRepository qaTests;
    private final StaffRepository staffRepository;
    private final AuditService auditService;
    private final Clock clock;

    public CalibrationService(ScannerService scannerService, CalibrationSessionRepository sessions,
                              CalibrationMeasurementRepository measurements, QaTestRepository qaTests,
                              StaffRepository staffRepository, AuditService auditService, Clock clock) {
        this.scannerService = scannerService;
        this.sessions = sessions;
        this.measurements = measurements;
        this.qaTests = qaTests;
        this.staffRepository = staffRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional
    public CalibrationDetail record(UUID scannerId, RecordCalibrationRequest request) {
        ScannerDevice scanner = scannerService.find(scannerId);
        if (scanner.getStatus() == ScannerStatus.RETIRED) {
            throw new ConflictException("Cannot record QA on a retired scanner");
        }
        if (request.performedAt().isAfter(clock.instant())) {
            throw new BadRequestException("performedAt must not be in the future", "performedAt");
        }
        CurrentUser actor = CurrentUser.require();
        Staff performer = staffRepository.findById(actor.id())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Account no longer exists"));
        CalibrationDetail detail = persist(scanner, request, performer);

        auditService.record(actor.actorType(), actor.id(), AuditAction.CALIBRATION_RECORDED,
                "CALIBRATION_SESSION", detail.id());
        return detail;
    }

    /**
     * Validates the QA tests, copies their limits, computes result, next due date and risk score, and saves.
     * No permission check or audit here: {@link #record} does those; the dev seeder calls this directly.
     */
    @Transactional
    public CalibrationDetail persist(ScannerDevice scanner, RecordCalibrationRequest request, Staff performer) {
        Map<UUID, QaTest> tests = loadTests(request.measurements());

        CalibrationSession session = new CalibrationSession();
        session.setScanner(scanner);
        session.setPerformedAt(request.performedAt());
        session.setPerformedBy(performer);
        session.setNotes(ScannerService.blankToNull(request.notes()));
        session.setCertificateRef(ScannerService.blankToNull(request.certificateRef()));

        List<CalibrationMeasurement> recorded = new ArrayList<>();
        for (RecordCalibrationRequest.Measurement m : request.measurements()) {
            QaTest test = tests.get(m.qaTestId());
            CalibrationMeasurement cm = new CalibrationMeasurement();
            cm.setSession(session);
            cm.setQaTest(test);
            cm.setMeasuredValue(m.measuredValue());
            // copy the limits so later edits of the QA test never rewrite history
            cm.setLowerLimit(test.getLowerLimit());
            cm.setUpperLimit(test.getUpperLimit());
            cm.setWithinTolerance(ToleranceEvaluator.isWithin(m.measuredValue(), test.getLowerLimit(), test.getUpperLimit()));
            recorded.add(cm);
        }
        boolean allWithin = recorded.stream().allMatch(CalibrationMeasurement::isWithinTolerance);
        session.setResult(allWithin ? CalibrationResult.PASS : CalibrationResult.FAIL);

        LocalDate performedDate = LocalDate.ofInstant(request.performedAt(), ZoneOffset.UTC);
        session.setNextDueDate(performedDate.plusDays(scanner.getCalibrationIntervalDays()));
        session.setRiskScore(riskScore(scanner, request, performedDate, recorded));

        session = sessions.save(session);
        measurements.saveAll(recorded);
        sessions.flush();
        return CalibrationDetail.from(session, recorded.stream().map(MeasurementResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<CalibrationSummary> history(UUID scannerId, int page, int size) {
        scannerService.find(scannerId);
        return PageResponse.of(sessions.findByScannerIdOrderByPerformedAtDescIdDesc(scannerId,
                Pages.of(page, size, Sort.unsorted())), CalibrationSummary::from);
    }

    @Transactional(readOnly = true)
    public CalibrationDetail get(UUID sessionId) {
        CalibrationSession session = sessions.findDetailed(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Calibration session", sessionId));
        List<MeasurementResponse> rows = measurements.findBySessionIdIn(List.of(sessionId)).stream()
                .sorted(Comparator.comparing((CalibrationMeasurement m) -> m.getQaTest().getCode()))
                .map(MeasurementResponse::from)
                .toList();
        return CalibrationDetail.from(session, rows);
    }

    /** 400 for an unknown or inactive test and for the same test twice in one session. */
    private Map<UUID, QaTest> loadTests(List<RecordCalibrationRequest.Measurement> rows) {
        Set<UUID> seen = new HashSet<>();
        for (RecordCalibrationRequest.Measurement m : rows) {
            if (!seen.add(m.qaTestId())) {
                throw new BadRequestException("Each QA test can be measured only once per session", "measurements");
            }
        }
        Map<UUID, QaTest> tests = qaTests.findAllById(seen).stream()
                .collect(Collectors.toMap(QaTest::getId, Function.identity()));
        for (UUID id : seen) {
            QaTest test = tests.get(id);
            if (test == null) {
                throw new BadRequestException("Unknown QA test: " + id, "measurements");
            }
            if (!test.isActive()) {
                throw new BadRequestException("QA test " + test.getCode() + " is inactive", "measurements");
            }
        }
        return tests;
    }

    /** The new session plus up to two earlier ones (by performed_at), and how late the new one was. */
    private double riskScore(ScannerDevice scanner, RecordCalibrationRequest request, LocalDate performedDate,
                             List<CalibrationMeasurement> recorded) {
        List<CalibrationSession> previous = sessions.findByScannerIdAndPerformedAtBeforeOrderByPerformedAtDescIdDesc(
                scanner.getId(), request.performedAt(), PageRequest.of(0, RISK_WINDOW - 1));
        Map<UUID, List<CalibrationMeasurement>> byPrevious = previous.isEmpty() ? Map.of()
                : measurements.findBySessionIdIn(previous.stream().map(CalibrationSession::getId).toList()).stream()
                .collect(Collectors.groupingBy(m -> m.getSession().getId()));

        List<List<RiskScoreCalculator.Point>> window = new ArrayList<>();
        window.add(points(recorded));
        for (CalibrationSession s : previous) {
            window.add(points(byPrevious.getOrDefault(s.getId(), List.of())));
        }
        long daysLate = previous.isEmpty() ? 0
                : QaStatusCalculator.daysOverdue(previous.get(0).getNextDueDate(), performedDate);
        return RiskScoreCalculator.score(window, daysLate, scanner.getCalibrationIntervalDays());
    }

    private static List<RiskScoreCalculator.Point> points(List<CalibrationMeasurement> ms) {
        return ms.stream().map(m -> new RiskScoreCalculator.Point(m.getMeasuredValue(), m.getLowerLimit(),
                m.getUpperLimit(), m.isWithinTolerance())).toList();
    }
}
