package com.neuroguard.seed;

import com.neuroguard.seed.service.SeedRunner;
import com.neuroguard.seed.service.SeedService;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the seed on an empty database and checks the dashboard against the expected totals documented in
 * docs/clinical-billing.md ("Seed data"). If you change the seed, update both.
 */
class SeedTest extends AbstractIntegrationTest {

    @Autowired
    private SeedService seedService;
    @Autowired
    private ApplicationContext context;

    @BeforeEach
    void emptyDatabase() {
        // remove the shared test fixtures so the seed runs on an empty database, as documented
        jdbc.execute("TRUNCATE TABLE audit_log, staff, patient, service CASCADE");
    }

    @AfterEach
    void cleanUp() {
        jdbc.execute("TRUNCATE TABLE service CASCADE");
    }

    private String token(String email) {
        Staff s = staffRepository.findByEmail(email).orElseThrow();
        return bearer(s);
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    @Test
    void seedIsIdempotent() {
        assertTrue(seedService.seed());
        Map<String, Integer> first = Map.of("staff", count("staff"), "patient", count("patient"),
                "invoice", count("invoice"), "payment", count("payment"));

        assertFalse(seedService.seed());
        assertEquals(first, Map.of("staff", count("staff"), "patient", count("patient"),
                "invoice", count("invoice"), "payment", count("payment")));
    }

    @Test
    void seedCreatesTheDocumentedRecords() {
        seedService.seed();
        assertEquals(6, count("staff"));
        assertEquals(10, count("patient"));
        assertEquals(4, count("service"));
        assertEquals(12, count("appointment"));
        assertEquals(8, count("mri_order"));
        assertEquals(9, count("diagnosis"));
        assertEquals(4, count("treatment_plan"));
        assertEquals(7, count("prescription"));
        assertEquals(7, count("invoice"));
        assertEquals(4, count("payment"));

        // one staff account per scope, plus the admin
        assertEquals(5, jdbc.queryForObject("SELECT count(DISTINCT permission_scope) FROM staff", Integer.class));
        // every person record is marked TEST
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM patient WHERE name NOT LIKE 'TEST %'", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM staff WHERE name NOT LIKE 'TEST %'", Integer.class));
        // invoices in every status, and each total equals the sum of its lines
        assertEquals(3, jdbc.queryForObject("SELECT count(DISTINCT status) FROM invoice", Integer.class));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM invoice i
                WHERE i.total_amount <> (SELECT sum(line_total) FROM invoice_item WHERE invoice_id = i.invoice_id)
                """, Integer.class));
        // appointments in every status
        assertEquals(4, jdbc.queryForObject("SELECT count(DISTINCT status) FROM appointment", Integer.class));
    }

    @Test
    void seededAccountsCanLogInWithTheDevPassword() throws Exception {
        seedService.seed();
        postJson("/api/auth/staff/login", Map.of("email", "test.neurologist@neuroguard.local",
                "password", SeedService.DEV_PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("NEUROLOGIST"));
        postJson("/api/auth/staff/login", Map.of("email", SeedService.MARKER_EMAIL, "password", SeedService.DEV_PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        postJson("/api/auth/patient/login", Map.of("email", "test.patient01@neuroguard.local",
                "password", SeedService.DEV_PASSWORD))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("PATIENT"));
    }

    @Test
    void dashboardMatchesTheDocumentedTotals() throws Exception {
        seedService.seed();
        String admin = token(SeedService.MARKER_EMAIL);

        get("/api/dashboard/patients/new-per-month", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].month", contains("2026-09", "2026-10", "2026-11")))
                .andExpect(jsonPath("$[*].count", contains(3, 3, 4)));

        get("/api/dashboard/diagnoses/by-category", admin)
                .andExpect(jsonPath("$[*].category", contains("G40", "G43", "C71", "G20", "G35", "I63")))
                .andExpect(jsonPath("$[*].count", contains(3, 2, 1, 1, 1, 1)));

        get("/api/dashboard/appointments/by-status", admin)
                .andExpect(jsonPath("$[*].status", contains("SCHEDULED", "COMPLETED", "CANCELLED", "MISSED")))
                .andExpect(jsonPath("$[*].count", contains(3, 6, 2, 1)));

        get("/api/dashboard/mri-orders/per-week", admin)
                .andExpect(jsonPath("$[*].isoWeek", contains("2026-W37", "2026-W39", "2026-W41", "2026-W43", "2026-W45")))
                .andExpect(jsonPath("$[*].count", contains(2, 1, 2, 1, 2)));

        get("/api/dashboard/finance", token("test.billing@neuroguard.local"))
                .andExpect(content().string(containsString("\"revenue\":10600.00")))
                .andExpect(jsonPath("$.completedPayments").value(4))
                .andExpect(jsonPath("$.unpaidCount").value(2))
                .andExpect(content().string(containsString("\"unpaidAmount\":9700.00")));

        get("/api/dashboard/finance?from=2026-11-01&to=2026-11-30", admin)
                .andExpect(content().string(containsString("\"revenue\":1600.00")))
                .andExpect(jsonPath("$.completedPayments").value(1))
                .andExpect(jsonPath("$.unpaidCount").value(1))
                .andExpect(content().string(containsString("\"unpaidAmount\":6400.00")));
    }

    @Test
    void runnerIsOnlyActiveUnderItsProfile() {
        assertTrue(context.getBeansOfType(SeedRunner.class).isEmpty());
    }
}
