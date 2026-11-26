package com.neuroguard.scanner.service;

import com.neuroguard.scanner.dto.RecordCalibrationRequest;
import com.neuroguard.scanner.entity.QaTest;
import com.neuroguard.scanner.entity.ScannerAccessory;
import com.neuroguard.scanner.entity.ScannerDevice;
import com.neuroguard.scanner.repository.QaTestRepository;
import com.neuroguard.scanner.repository.ScannerAccessoryRepository;
import com.neuroguard.scanner.repository.ScannerDeviceRepository;
import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dev-only demo data for the scanner QA module. Everything is synthetic and prefixed TEST. Runs once:
 * it does nothing if a "TEST" scanner already exists. Never runs in the test profile.
 *
 * <p>The ACR large-phantom tolerances below are <b>example placeholder values</b> (is_placeholder = true)
 * so the demo works until the team decides on real tolerances. They must be confirmed by a medical
 * physicist and are not to be used clinically.
 */
@Component
@Profile("dev")
public class ScannerDemoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ScannerDemoSeeder.class);

    static final String PLACEHOLDER_REFERENCE = "Placeholder: confirm with medical physicist. Example value "
            + "based on commonly cited ACR large-phantom criteria; not for clinical use.";

    private final ScannerDeviceRepository scanners;
    private final ScannerAccessoryRepository accessories;
    private final QaTestRepository qaTests;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final CalibrationService calibrationService;
    private final Clock clock;

    public ScannerDemoSeeder(ScannerDeviceRepository scanners, ScannerAccessoryRepository accessories,
                             QaTestRepository qaTests, StaffRepository staffRepository,
                             PasswordEncoder passwordEncoder, CalibrationService calibrationService, Clock clock) {
        this.scanners = scanners;
        this.accessories = accessories;
        this.qaTests = qaTests;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.calibrationService = calibrationService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (scanners.existsByNameStartingWith("TEST")) {
            return;
        }
        Map<String, QaTest> tests = seedQaTests();
        Staff engineer = seedEngineer();

        ScannerDevice a = scanner("TEST MRI-1 (1.5 T)", "TEST-SN-15T-0001", 1.5, "TEST Station A", "TEST Radiology, room 1");
        coil(a, "TEST Head coil 8ch", "Head", "TEST-COIL-0001");
        coil(a, "TEST Spine coil", "Spine", "TEST-COIL-0002");

        ScannerDevice b = scanner("TEST MRI-2 (3 T)", "TEST-SN-30T-0001", 3.0, "TEST Station B", "TEST Radiology, room 2");
        coil(b, "TEST Head/Neck coil 20ch", "Head/Neck", "TEST-COIL-0003");
        coil(b, "TEST Knee coil", "Knee", "TEST-COIL-0004");

        // Scanner A: three passing sessions, latest 20 days ago with a 90-day interval -> OK
        session(a, engineer, 200, tests, values(190.2, 0.9, 5.1, 1.0, 92.0, 0.9, 10), "TEST routine QA");
        session(a, engineer, 110, tests, values(189.8, 0.9, 4.9, -1.0, 91.5, 1.0, 10), "TEST routine QA");
        session(a, engineer, 20, tests, values(190.0, 0.9, 5.0, 0.5, 92.5, 0.8, 11), "TEST routine QA");

        // Scanner B: one failing session (ghosting), then a pass 130 days ago -> due 40 days ago -> OVERDUE
        session(b, engineer, 230, tests, values(190.5, 1.0, 5.4, 2.0, 88.5, 3.1, 9), "TEST ghosting above limit");
        session(b, engineer, 130, tests, values(191.6, 1.0, 5.6, 4.0, 88.0, 2.2, 9), "TEST after repair, values near limits");

        log.info("Seeded scanner QA demo data (TEST scanners, ACR placeholder tests, calibration history)");
    }

    private Map<String, QaTest> seedQaTests() {
        Map<String, QaTest> map = new LinkedHashMap<>();
        map.put("ACR_GEOM", qaTest("ACR_GEOM", "Geometric accuracy", "mm", "188", "192"));
        map.put("ACR_HCSR", qaTest("ACR_HCSR", "High-contrast spatial resolution", "mm", null, "1.0"));
        map.put("ACR_SLICE_THK", qaTest("ACR_SLICE_THK", "Slice thickness accuracy", "mm", "4.3", "5.7"));
        map.put("ACR_SLICE_POS", qaTest("ACR_SLICE_POS", "Slice position accuracy", "mm", "-5", "5"));
        map.put("ACR_PIU", qaTest("ACR_PIU", "Image intensity uniformity (PIU)", "%", "87.5", null));
        map.put("ACR_GHOST", qaTest("ACR_GHOST", "Percent signal ghosting", "%", null, "2.5"));
        map.put("ACR_LCD", qaTest("ACR_LCD", "Low-contrast detectability", "spokes", "9", null));
        return map;
    }

    private QaTest qaTest(String code, String name, String unit, String lower, String upper) {
        return qaTests.findAll().stream().filter(t -> t.getCode().equals(code)).findFirst().orElseGet(() -> {
            QaTest t = new QaTest();
            t.setCode(code);
            t.setName(name);
            t.setUnit(unit);
            t.setLowerLimit(lower == null ? null : new BigDecimal(lower));
            t.setUpperLimit(upper == null ? null : new BigDecimal(upper));
            t.setReference(PLACEHOLDER_REFERENCE);
            t.setPlaceholder(true);
            t.setActive(true);
            return qaTests.save(t);
        });
    }

    /** Uses an existing biomedical engineer, or creates a TEST one that cannot log in (random password). */
    private Staff seedEngineer() {
        return staffRepository.findAll().stream()
                .filter(s -> s.getPermissionScope() == PermissionScope.BIOMEDICAL_ENGINEER)
                .findFirst()
                .orElseGet(() -> {
                    Staff s = new Staff();
                    s.setName("TEST Biomedical Engineer (seed)");
                    s.setEmail("test.biomed.seed@neuroguard.invalid");
                    s.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
                    s.setBaseRole(BaseRole.CLINICAL_STAFF);
                    s.setPermissionScope(PermissionScope.BIOMEDICAL_ENGINEER);
                    return staffRepository.save(s);
                });
    }

    private ScannerDevice scanner(String name, String serial, double tesla, String station, String location) {
        ScannerDevice s = new ScannerDevice();
        s.setName(name);
        s.setManufacturer("TEST Manufacturer");
        s.setModel(tesla < 2 ? "TEST Model 15" : "TEST Model 30");
        s.setSerialNumber(serial);
        s.setStationName(station);
        s.setFieldStrength(tesla);
        s.setLocation(location);
        s.setInstallDate(LocalDate.now(clock).minusYears(3));
        s.setCalibrationIntervalDays(90);
        return scanners.save(s);
    }

    private void coil(ScannerDevice scanner, String name, String type, String serial) {
        ScannerAccessory a = new ScannerAccessory();
        a.setScanner(scanner);
        a.setName(name);
        a.setType(type);
        a.setSerialNumber(serial);
        accessories.save(a);
    }

    /** Values in seed-test order: GEOM, HCSR, SLICE_THK, SLICE_POS, PIU, GHOST, LCD. */
    private static double[] values(double... v) {
        return v;
    }

    private void session(ScannerDevice scanner, Staff engineer, int daysAgo, Map<String, QaTest> tests,
                         double[] values, String notes) {
        List<RecordCalibrationRequest.Measurement> rows = new ArrayList<>();
        int i = 0;
        for (QaTest t : tests.values()) {
            rows.add(new RecordCalibrationRequest.Measurement(t.getId(), BigDecimal.valueOf(values[i++])));
        }
        Instant when = clock.instant().minus(Duration.ofDays(daysAgo));
        calibrationService.persist(scanner, new RecordCalibrationRequest(when, notes, "TEST-CERT-" + daysAgo, rows), engineer);
    }
}
