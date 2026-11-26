package com.neuroguard.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Derived QA status with a fixed clock (interval 90 days, DUE_SOON window 30 days). */
class ScannerQaStatusTest extends ScannerTestSupport {

    private static final Instant T0 = Instant.parse("2026-03-01T09:00:00Z");

    private String scannerId;
    private String geom;

    @BeforeEach
    void data() throws Exception {
        testClock.setInstant(T0);
        scannerId = createScanner("TEST MRI-1", "TEST-SN-1", 90);
        geom = createQaTest("TEST_GEOM", 188, 192);
    }

    private String qaStatus() throws Exception {
        return json(get("/api/scanners/" + scannerId, bearer(neurologist)).andExpect(status().isOk()))
                .get("qaStatus").asText();
    }

    private void daysLater(long days) {
        testClock.setInstant(T0.plus(Duration.ofDays(days)));
    }

    @Test
    void neverCalibratedThenOkThenDueSoonThenOverdue() throws Exception {
        assertEquals("NEVER_CALIBRATED", qaStatus());

        recordOk(scannerId, T0.minusSeconds(60), geom, 190); // due 2026-05-30
        assertEquals("OK", qaStatus());

        daysLater(59);                                           // 31 days left
        assertEquals("OK", qaStatus());
        daysLater(60);                                           // 30 days left
        assertEquals("DUE_SOON", qaStatus());
        daysLater(90);                                           // due today
        assertEquals("DUE_SOON", qaStatus());
        daysLater(91);                                           // one day past
        assertEquals("OVERDUE", qaStatus());
    }

    @Test
    void latestFailedSessionIsFailedAndFlaggedForReview() throws Exception {
        recordOk(scannerId, T0.minusSeconds(120), geom, 190);
        recordOk(scannerId, T0.minusSeconds(60), geom, 200);
        assertEquals("FAILED", qaStatus());

        get("/api/scanners/" + scannerId + "/qa-summary", bearer(mriTech))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qaStatus").value("FAILED"))
                .andExpect(jsonPath("$.flaggedForReview").value(true))
                .andExpect(jsonPath("$.lastQaResult").value("FAIL"));

        // a later passing session clears it
        recordOk(scannerId, T0.minusSeconds(30), geom, 190);
        assertEquals("OK", qaStatus());
    }

    @Test
    void latestIsByPerformedAtNotByRecordingOrder() throws Exception {
        recordOk(scannerId, T0.minusSeconds(60), geom, 190);              // newest, PASS
        recordOk(scannerId, T0.minus(Duration.ofDays(10)), geom, 200);    // back-dated FAIL
        assertEquals("OK", qaStatus());
        get("/api/scanners/" + scannerId + "/calibrations", bearer(radiologist))
                .andExpect(jsonPath("$.content[0].result").value("PASS"))
                .andExpect(jsonPath("$.content[1].result").value("FAIL"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void qaSummaryCarriesProvenance() throws Exception {
        get("/api/scanners/" + scannerId + "/qa-summary", bearer(radiologist))
                .andExpect(jsonPath("$.qaStatus").value("NEVER_CALIBRATED"))
                .andExpect(jsonPath("$.flaggedForReview").value(false))
                .andExpect(jsonPath("$.sourceSessionId").doesNotExist());

        JsonNode session = recordOk(scannerId, T0.minusSeconds(60), geom, 190);
        daysLater(100);
        get("/api/scanners/" + scannerId + "/qa-summary", bearer(radiologist))
                .andExpect(jsonPath("$.scannerId").value(scannerId))
                .andExpect(jsonPath("$.scannerName").value("TEST MRI-1"))
                .andExpect(jsonPath("$.fieldStrength").value(1.5))
                .andExpect(jsonPath("$.qaStatus").value("OVERDUE"))
                .andExpect(jsonPath("$.flaggedForReview").value(true))
                .andExpect(jsonPath("$.sourceSessionId").value(session.get("id").asText()))
                .andExpect(jsonPath("$.nextDueDate").value("2026-05-30"))
                .andExpect(jsonPath("$.riskScore").value(session.get("riskScore").asDouble()))
                .andExpect(jsonPath("$.evaluatedOn").value("2026-06-09"));
    }

    @Test
    void listFiltersAndOverviewCounts() throws Exception {
        String failed = createScanner("TEST MRI-2", "TEST-SN-2", 90);
        createScanner("TEST MRI-3", "TEST-SN-3", 90); // never calibrated
        String retired = createScanner("TEST MRI-4", "TEST-SN-4", 90);
        recordOk(scannerId, T0.minusSeconds(60), geom, 190);
        recordOk(failed, T0.minusSeconds(60), geom, 200);
        patch("/api/scanners/" + retired + "/status", bearer(biomedEngineer), Map.of("status", "RETIRED"))
                .andExpect(status().isOk());

        get("/api/scanners", bearer(admin))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].name").value("TEST MRI-1"))
                .andExpect(jsonPath("$.content[0].qaStatus").value("OK"))
                .andExpect(jsonPath("$.content[0].lastQaResult").value("PASS"))
                .andExpect(jsonPath("$.content[0].riskScore").isNumber());
        get("/api/scanners?qaStatus=FAILED", bearer(admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("TEST MRI-2"));
        get("/api/scanners?status=RETIRED", bearer(admin))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("TEST MRI-4"));
        get("/api/scanners?size=2&page=1", bearer(admin))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));

        get("/api/scanners/qa-overview", bearer(admin))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.ok").value(1))
                .andExpect(jsonPath("$.failed").value(1))
                .andExpect(jsonPath("$.neverCalibrated").value(1))
                .andExpect(jsonPath("$.overdue").value(0))
                .andExpect(jsonPath("$.dueSoon").value(0));
    }
}
