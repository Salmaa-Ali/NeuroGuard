package com.neuroguard.scanner;

import com.neuroguard.staff.entity.Staff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Scanners & calibration: Admin/Neurologist/Radiologist/MRI tech R, Biomedical engineer RW, others none. */
class ScannerPermissionTest extends ScannerTestSupport {

    private String scannerId;
    private String testId;
    private String sessionId;

    @BeforeEach
    void data() throws Exception {
        scannerId = createScanner("TEST MRI-1", "TEST-SN-1", 90);
        testId = createQaTest("TEST_GEOM", 188, 192);
        sessionId = recordOk(scannerId, Instant.now().minus(1, ChronoUnit.HOURS), testId, 190).get("id").asText();
    }

    private List<String> readUrls() {
        return List.of("/api/scanners", "/api/scanners/" + scannerId, "/api/scanners/" + scannerId + "/calibrations",
                "/api/scanners/" + scannerId + "/qa-summary", "/api/scanners/qa-overview", "/api/qa-tests",
                "/api/calibrations/" + sessionId);
    }

    @Test
    void readersCanReadEverything() throws Exception {
        for (Staff reader : List.of(admin, neurologist, radiologist, mriTech, biomedEngineer)) {
            for (String url : readUrls()) {
                get(url, bearer(reader)).andExpect(status().isOk());
            }
        }
    }

    @Test
    void billingClerkAndPatientCannotRead() throws Exception {
        for (String url : readUrls()) {
            assertStandardError(get(url, bearer(billingClerk)), 403);
            assertStandardError(get(url, bearer(patientA)), 403);
        }
    }

    @Test
    void readOnlyRolesGet403OnEveryWrite() throws Exception {
        for (Staff reader : List.of(admin, neurologist, radiologist, mriTech, billingClerk)) {
            String t = bearer(reader);
            assertStandardError(postJson("/api/scanners", t, scannerBody("TEST X", "TEST-SN-X", 30)), 403);
            assertStandardError(put("/api/scanners/" + scannerId, t, scannerBody("TEST X", "TEST-SN-X", 30)), 403);
            assertStandardError(patch("/api/scanners/" + scannerId + "/status", t, Map.of("status", "RETIRED")), 403);
            assertStandardError(postJson("/api/scanners/" + scannerId + "/accessories", t, Map.of("name", "TEST coil")), 403);
            assertStandardError(postJson("/api/scanners/" + scannerId + "/calibrations", t,
                    sessionBody(Instant.now().minusSeconds(60), testId, 190)), 403);
            assertStandardError(postJson("/api/qa-tests", t, qaTestBody("TEST_NEW", 1, 2)), 403);
            assertStandardError(put("/api/qa-tests/" + testId, t, qaTestBody("TEST_GEOM", 1, 2)), 403);
        }
        assertStandardError(postJson("/api/scanners", bearer(patientA), scannerBody("TEST X", "TEST-SN-X", 30)), 403);
    }

    @Test
    void biomedicalEngineerCanWriteAndEveryChangeIsAudited() throws Exception {
        String t = bearer(biomedEngineer);
        put("/api/scanners/" + scannerId, t, scannerBody("TEST MRI-1 renamed", "TEST-SN-1", 60))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("TEST MRI-1 renamed"))
                .andExpect(jsonPath("$.calibrationIntervalDays").value(60));
        patch("/api/scanners/" + scannerId + "/status", t, Map.of("status", "OUT_OF_SERVICE"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));

        String coilId = id(postJson("/api/scanners/" + scannerId + "/accessories", t,
                Map.of("name", "TEST Head coil", "type", "Head", "serialNumber", "TEST-C1"))
                .andExpect(status().isCreated()));
        put("/api/scanners/" + scannerId + "/accessories/" + coilId, t, Map.of("name", "TEST Head coil 16ch"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("TEST Head coil 16ch"));
        get("/api/scanners/" + scannerId, t).andExpect(jsonPath("$.accessories.length()").value(1));
        delete("/api/scanners/" + scannerId + "/accessories/" + coilId, t).andExpect(status().isNoContent());
        get("/api/scanners/" + scannerId, t).andExpect(jsonPath("$.accessories.length()").value(0));

        put("/api/qa-tests/" + testId, t, qaTestBody("TEST_GEOM", 187, 193)).andExpect(status().isOk());

        assertEquals(1, auditCount("SCANNER_CREATED"));
        assertEquals(4, auditCount("SCANNER_UPDATED")); // PUT + accessory add, update, delete
        assertEquals(1, auditCount("SCANNER_STATUS_CHANGED"));
        assertEquals(2, auditCount("QA_TEST_CHANGED"));  // create + update
        assertEquals(1, auditCount("CALIBRATION_RECORDED"));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'CALIBRATION_RECORDED' AND actor_id = ?
                  AND entity_type = 'CALIBRATION_SESSION' AND entity_id = ?::uuid
                """, Integer.class, biomedEngineer.getId(), sessionId));
    }

    @Test
    void noTokenGets401() throws Exception {
        assertStandardError(mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/scanners")), 401);
    }
}
