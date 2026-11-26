package com.neuroguard.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recording sessions: server-computed result, due date and risk; copied limits; validation errors. */
class CalibrationRecordingTest extends ScannerTestSupport {

    private static final Instant NOW = Instant.parse("2026-06-15T10:00:00Z");

    private String scannerId;
    private String geom;   // two-sided 188..192
    private String ghost;  // upper only <= 2.5
    private String piu;    // lower only >= 87.5

    @BeforeEach
    void data() throws Exception {
        testClock.setInstant(NOW);
        scannerId = createScanner("TEST MRI-1", "TEST-SN-1", 90);
        geom = createQaTest("TEST_GEOM", 188, 192);
        ghost = createQaTest("TEST_GHOST", null, 2.5);
        piu = createQaTest("TEST_PIU", 87.5, null);
    }

    @Test
    void allWithinToleranceIsPassWithDueDateRiskAndPerformer() throws Exception {
        ResultActions result = record(scannerId, NOW.minusSeconds(3600), geom, 190, ghost, 1.0, piu, 95)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/calibrations/")))
                .andExpect(jsonPath("$.result").value("PASS"))
                .andExpect(jsonPath("$.nextDueDate").value("2026-09-13")) // 2026-06-15 + 90 days
                .andExpect(jsonPath("$.performedById").value(biomedEngineer.getId().toString()))
                .andExpect(jsonPath("$.measurements.length()").value(3));
        double risk = json(result).get("riskScore").asDouble();
        assertTrue(risk >= 0 && risk <= 100);
        assertNoPasswordHash(result);
    }

    @Test
    void oneValueOutsideToleranceMakesTheSessionFail() throws Exception {
        JsonNode session = recordOk(scannerId, NOW.minusSeconds(60), geom, 190, ghost, 3.0, piu, 95);
        assertEquals("FAIL", session.get("result").asText());
        for (JsonNode m : session.get("measurements")) {
            assertEquals(!m.get("qaTestCode").asText().equals("TEST_GHOST"), m.get("withinTolerance").asBoolean());
        }
    }

    @Test
    void boundaryValuesAndOneSidedLimitsPass() throws Exception {
        assertEquals("PASS", recordOk(scannerId, NOW.minusSeconds(300), geom, 188, ghost, 2.5, piu, 87.5)
                .get("result").asText());
        assertEquals("PASS", recordOk(scannerId, NOW.minusSeconds(200), geom, 192, ghost, -100, piu, 100000)
                .get("result").asText());
        assertEquals("FAIL", recordOk(scannerId, NOW.minusSeconds(100), piu, 87.4999).get("result").asText());
        assertEquals("FAIL", recordOk(scannerId, NOW.minusSeconds(50), geom, 192.0001).get("result").asText());
    }

    @Test
    void testWithoutLimitsAlwaysPasses() throws Exception {
        String free = createQaTest("TEST_FREE", null, null);
        assertEquals("PASS", recordOk(scannerId, NOW.minusSeconds(60), free, -9999).get("result").asText());
    }

    @Test
    void riskIsHigherAfterFailuresAndLateness() throws Exception {
        double good = recordOk(scannerId, NOW.minusSeconds(3000), geom, 190).get("riskScore").asDouble();

        String other = createScanner("TEST MRI-2", "TEST-SN-2", 90);
        double bad = recordOk(other, NOW.minusSeconds(3000), geom, 199).get("riskScore").asDouble();
        assertTrue(bad > good, bad + " > " + good);

        // a third scanner: on-time pass vs the same pass done 60 days after its due date
        String third = createScanner("TEST MRI-3", "TEST-SN-3", 30);
        testClock.setInstant(NOW.minusSeconds(86400L * 200));
        recordOk(third, NOW.minusSeconds(86400L * 200), geom, 190);
        testClock.setInstant(NOW);
        double late = recordOk(third, NOW.minusSeconds(60), geom, 190).get("riskScore").asDouble();
        assertTrue(late > good, late + " > " + good);
    }

    @Test
    void measurementLimitsAreCopiedSoEditingTheTestDoesNotChangeHistory() throws Exception {
        String sessionId = recordOk(scannerId, NOW.minusSeconds(60), geom, 191).get("id").asText();

        put("/api/qa-tests/" + geom, bearer(biomedEngineer), qaTestBody("TEST_GEOM", 189, 190))
                .andExpect(status().isOk());

        get("/api/calibrations/" + sessionId, bearer(neurologist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("PASS"))
                .andExpect(jsonPath("$.measurements[0].lowerLimit").value(188))
                .andExpect(jsonPath("$.measurements[0].upperLimit").value(192))
                .andExpect(jsonPath("$.measurements[0].withinTolerance").value(true));

        // a new session uses the new limits
        assertEquals("FAIL", recordOk(scannerId, NOW.minusSeconds(30), geom, 191).get("result").asText());
    }

    @Test
    void clientCannotSetResultRiskOrPerformer() throws Exception {
        Map<String, Object> body = sessionBody(NOW.minusSeconds(60), geom, 300);
        body.put("result", "PASS");
        body.put("riskScore", 0);
        body.put("performedBy", admin.getId().toString());
        postJson("/api/scanners/" + scannerId + "/calibrations", bearer(biomedEngineer), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result").value("FAIL"))
                .andExpect(jsonPath("$.performedById").value(biomedEngineer.getId().toString()));
    }

    // ---- validation ----

    @Test
    void duplicateSerialNumberReturns409() throws Exception {
        assertStandardError(postJson("/api/scanners", bearer(biomedEngineer), scannerBody("TEST other", "TEST-SN-1", 30)), 409);
        String second = createScanner("TEST MRI-2", "TEST-SN-2", 30);
        assertStandardError(put("/api/scanners/" + second, bearer(biomedEngineer), scannerBody("TEST MRI-2", "TEST-SN-1", 30)), 409);
    }

    @Test
    void unknownOrInactiveQaTestReturns400() throws Exception {
        assertStandardError(record(scannerId, NOW.minusSeconds(60), UUID.randomUUID().toString(), 1), 400);

        Map<String, Object> inactive = qaTestBody("TEST_GEOM", 188, 192);
        inactive.put("active", false);
        put("/api/qa-tests/" + geom, bearer(biomedEngineer), inactive).andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        assertStandardError(record(scannerId, NOW.minusSeconds(60), geom, 190), 400);
        get("/api/qa-tests?activeOnly=true", bearer(biomedEngineer)).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void sameTestTwiceInOneSessionReturns400() throws Exception {
        assertStandardError(record(scannerId, NOW.minusSeconds(60), geom, 190, geom, 191), 400);
    }

    @Test
    void performedAtInTheFutureReturns400() throws Exception {
        ResultActions result = record(scannerId, NOW.plusSeconds(60), geom, 190);
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.fieldErrors[0].field").value("performedAt"));
    }

    @Test
    void retiredScannerReturns409() throws Exception {
        patch("/api/scanners/" + scannerId + "/status", bearer(biomedEngineer), Map.of("status", "RETIRED"))
                .andExpect(status().isOk());
        assertStandardError(record(scannerId, NOW.minusSeconds(60), geom, 190), 409);
    }

    @Test
    void emptyMeasurementsAndMissingFieldsReturn400() throws Exception {
        assertStandardError(record(scannerId, NOW.minusSeconds(60)), 400);
        Map<String, Object> scanner = scannerBody("TEST X", "TEST-SN-X", 30);
        scanner.remove("calibrationIntervalDays");
        assertStandardError(postJson("/api/scanners", bearer(biomedEngineer), scanner), 400);
        scanner.put("calibrationIntervalDays", 0);
        assertStandardError(postJson("/api/scanners", bearer(biomedEngineer), scanner), 400);
    }

    @Test
    void qaTestRules() throws Exception {
        assertStandardError(postJson("/api/qa-tests", bearer(biomedEngineer), qaTestBody("TEST_BAD", 5, 1)), 400);
        assertStandardError(postJson("/api/qa-tests", bearer(biomedEngineer), qaTestBody("test_geom", 1, 2)), 409);
    }

    @Test
    void unknownIdsReturn404() throws Exception {
        assertStandardError(get("/api/scanners/" + UUID.randomUUID(), bearer(biomedEngineer)), 404);
        assertStandardError(get("/api/calibrations/" + UUID.randomUUID(), bearer(biomedEngineer)), 404);
        assertStandardError(record(UUID.randomUUID().toString(), NOW.minusSeconds(60), geom, 190), 404);
    }
}
