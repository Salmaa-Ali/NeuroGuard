package com.neuroguard.scanner;

import com.fasterxml.jackson.databind.JsonNode;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Helpers for the scanner QA integration tests. All test data is synthetic. */
abstract class ScannerTestSupport extends AbstractIntegrationTest {

    @BeforeEach
    void cleanScannerTables() {
        // scanner tables are not part of the base fixture TRUNCATE
        jdbc.execute("TRUNCATE TABLE calibration_measurement, calibration_session, scanner_accessory, "
                + "scanner_device, qa_test CASCADE");
    }

    protected Map<String, Object> scannerBody(String name, String serial, int intervalDays) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("manufacturer", "TEST Manufacturer");
        body.put("model", "TEST Model");
        body.put("serialNumber", serial);
        body.put("stationName", "TEST Station");
        body.put("fieldStrength", 1.5);
        body.put("location", "TEST Room 1");
        body.put("installDate", "2024-01-15");
        body.put("calibrationIntervalDays", intervalDays);
        return body;
    }

    protected String createScanner(String name, String serial, int intervalDays) throws Exception {
        return id(postJson("/api/scanners", bearer(biomedEngineer), scannerBody(name, serial, intervalDays))
                .andExpect(status().isCreated()));
    }

    protected Map<String, Object> qaTestBody(String code, Object lower, Object upper) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("name", "TEST " + code);
        body.put("unit", "mm");
        body.put("lowerLimit", lower);
        body.put("upperLimit", upper);
        body.put("reference", "TEST reference");
        body.put("placeholder", false);
        body.put("active", true);
        return body;
    }

    protected String createQaTest(String code, Object lower, Object upper) throws Exception {
        return id(postJson("/api/qa-tests", bearer(biomedEngineer), qaTestBody(code, lower, upper))
                .andExpect(status().isCreated()));
    }

    /** measurements: qaTestId, value, qaTestId, value, ... */
    protected Map<String, Object> sessionBody(Instant performedAt, Object... measurements) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int i = 0; i < measurements.length; i += 2) {
            Map<String, Object> m = new HashMap<>();
            m.put("qaTestId", measurements[i]);
            m.put("measuredValue", measurements[i + 1]);
            rows.add(m);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("performedAt", performedAt.toString());
        body.put("notes", "TEST notes");
        body.put("certificateRef", "TEST-CERT");
        body.put("measurements", rows);
        return body;
    }

    protected ResultActions record(String scannerId, Instant performedAt, Object... measurements) throws Exception {
        return postJson("/api/scanners/" + scannerId + "/calibrations", bearer(biomedEngineer),
                sessionBody(performedAt, measurements));
    }

    protected JsonNode recordOk(String scannerId, Instant performedAt, Object... measurements) throws Exception {
        return json(record(scannerId, performedAt, measurements).andExpect(status().isCreated()));
    }

    protected ResultActions put(String url, String token, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(url).header("Authorization", token)
                .contentType("application/json").content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions patch(String url, String token, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.patch(url).header("Authorization", token)
                .contentType("application/json").content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions delete(String url, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.delete(url).header("Authorization", token));
    }

    protected JsonNode json(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }

    protected String id(ResultActions result) throws Exception {
        return json(result).get("id").asText();
    }

    protected int auditCount(String action) {
        return jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = ?", Integer.class, action);
    }
}
