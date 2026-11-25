package com.neuroguard.dashboard;

import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Known data set (all UTC, 2026), expected figures worked out by hand:
 * <ul>
 *   <li>Patients created: Sep 2 (A, B), Oct 1 (Oct 31 23:59:59), Nov 3 (Nov 1 00:00, Nov 20, Nov 21)</li>
 *   <li>Diagnoses: G40 x2 + G40909 x1 (category G40 = 3), G43 x2, C71 x1; Nov only: C71, G40, G43 one each</li>
 *   <li>Appointments: SCHEDULED 2 (Nov 28, Dec 2), COMPLETED 3 (Sep 10, Oct 5, Nov 15), CANCELLED 1 (Oct 12), MISSED 0</li>
 *   <li>MRI orders: W35 1 (Aug 30), W38 2 (Sep 14, Sep 20 23:30), W39 1 (Sep 21 00:00), W45 3 (Nov 2, 4, 8)</li>
 *   <li>Payments COMPLETED 1400.00 (Oct 10) + 800.00 (Nov 6) = 2200.00; a REFUNDED 500.00 is ignored.
 *       Unpaid invoices 300.00 (Oct 20) + 2500.00 (Nov 15) = 2800.00; a CANCELLED 3500.00 is ignored.</li>
 * </ul>
 */
class DashboardTest extends AbstractIntegrationTest {

    private static final String URL = "/api/dashboard";

    @BeforeEach
    void knownData() {
        // patients (A and B come from the fixture; their created_at is moved into the data set)
        jdbc.update("UPDATE patient SET created_at = ? WHERE patient_id = ?", ts("2026-09-10T10:00:00Z"), patientA.getId());
        jdbc.update("UPDATE patient SET created_at = ? WHERE patient_id = ?", ts("2026-09-25T10:00:00Z"), patientB.getId());
        patient(3, "2026-10-31T23:59:59Z");
        patient(4, "2026-11-01T00:00:00Z");
        patient(5, "2026-11-20T12:00:00Z");
        patient(6, "2026-11-21T12:00:00Z");

        // diagnoses
        jdbc.update("INSERT INTO icd10_code (code, description, category) VALUES ('G40909', "
                + "'Epilepsy, unspecified, not intractable, without status epilepticus', 'G40')");
        diagnosis("G40", "2026-09-15T09:00:00Z");
        diagnosis("G40909", "2026-10-02T09:00:00Z");
        diagnosis("G43", "2026-10-20T09:00:00Z");
        diagnosis("G43", "2026-11-03T09:00:00Z");
        diagnosis("C71", "2026-11-10T09:00:00Z");
        diagnosis("G40", "2026-11-12T09:00:00Z");

        // appointments
        appointment("SCHEDULED", "2026-11-28T09:00:00Z");
        appointment("SCHEDULED", "2026-12-02T09:00:00Z");
        appointment("COMPLETED", "2026-09-10T09:00:00Z");
        appointment("COMPLETED", "2026-10-05T09:00:00Z");
        UUID forMri = appointment("COMPLETED", "2026-11-15T09:00:00Z");
        appointment("CANCELLED", "2026-10-12T09:00:00Z");

        // MRI orders
        String[] orders = {"2026-08-30T12:00:00Z", "2026-09-14T08:00:00Z", "2026-09-20T23:30:00Z",
                "2026-09-21T00:00:00Z", "2026-11-02T10:00:00Z", "2026-11-04T10:00:00Z", "2026-11-08T22:00:00Z"};
        for (int i = 0; i < orders.length; i++) {
            jdbc.update("INSERT INTO mri_order (appointment_id, patient_id, accession_number, created_at) VALUES (?, ?, ?, ?)",
                    forMri, patientA.getId(), "TEST-ACC-" + i, ts(orders[i]));
        }

        // invoices and payments
        payment(invoice("PAID", "1400.00", "2026-10-10"), "1400.00", "COMPLETED", "2026-10-10T12:00:00Z");
        payment(invoice("PAID", "800.00", "2026-11-05"), "800.00", "COMPLETED", "2026-11-06T09:00:00Z");
        payment(invoice("PAID", "500.00", "2026-11-07"), "500.00", "REFUNDED", "2026-11-07T09:00:00Z");
        invoice("UNPAID", "300.00", "2026-10-20");
        invoice("UNPAID", "2500.00", "2026-11-15");
        invoice("CANCELLED", "3500.00", "2026-11-16");
    }

    @AfterEach
    void removeExtraCode() {
        jdbc.update("DELETE FROM diagnosis");
        jdbc.update("DELETE FROM icd10_code WHERE code = 'G40909'");
    }

    // ---- helpers ----

    private static Timestamp ts(String instant) {
        return Timestamp.from(Instant.parse(instant));
    }

    private void patient(int n, String createdAt) {
        jdbc.update("INSERT INTO patient (patient_number, email, password_hash, name, date_of_birth, created_at) "
                        + "VALUES (?, ?, 'x', ?, DATE '1990-01-01', ?)",
                "TEST-D" + n, "dash" + n + "@neuroguard.test", "TEST Dashboard " + n, ts(createdAt));
    }

    private void diagnosis(String code, String at) {
        jdbc.update("INSERT INTO diagnosis (patient_id, staff_id, icd10_code, diagnosed_at) VALUES (?, ?, ?, ?)",
                patientA.getId(), neurologist.getId(), code, ts(at));
    }

    private UUID appointment(String status, String at) {
        return jdbc.queryForObject("INSERT INTO appointment (patient_id, staff_id, scheduled_time, status) "
                + "VALUES (?, ?, ?, ?) RETURNING appointment_id", UUID.class,
                patientA.getId(), neurologist.getId(), ts(at), status);
    }

    private UUID invoice(String status, String total, String issueDate) {
        return jdbc.queryForObject("INSERT INTO invoice (patient_id, total_amount, status, issue_date) "
                + "VALUES (?, ?::numeric, ?, ?) RETURNING invoice_id", UUID.class,
                patientB.getId(), total, status, Date.valueOf(LocalDate.parse(issueDate)));
    }

    private void payment(UUID invoiceId, String amount, String status, String at) {
        jdbc.update("INSERT INTO payment (invoice_id, amount_paid, status, payment_date) VALUES (?, ?::numeric, ?, ?)",
                invoiceId, amount, status, ts(at));
    }

    // ---- figures ----

    @Test
    void newPatientsPerMonth() throws Exception {
        get(URL + "/patients/new-per-month", bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].month", contains("2026-09", "2026-10", "2026-11")))
                .andExpect(jsonPath("$[*].count", contains(2, 1, 3)));
        // to is inclusive up to 23:59:59.999 UTC; Nov 1 00:00 falls outside October
        get(URL + "/patients/new-per-month?from=2026-10-01&to=2026-10-31", bearer(admin))
                .andExpect(jsonPath("$[*].month", contains("2026-10")))
                .andExpect(jsonPath("$[*].count", contains(1)));
        get(URL + "/patients/new-per-month?from=2026-11-01", bearer(admin))
                .andExpect(jsonPath("$[*].count", contains(3)));
    }

    @Test
    void diagnosesGroupedByIcd10Category() throws Exception {
        get(URL + "/diagnoses/by-category", bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category", contains("G40", "G43", "C71")))
                .andExpect(jsonPath("$[*].count", contains(3, 2, 1)))
                .andExpect(jsonPath("$[0].description").value("Epilepsy and recurrent seizures"));
        // equal counts are ordered by category
        get(URL + "/diagnoses/by-category?from=2026-11-01&to=2026-11-30", bearer(admin))
                .andExpect(jsonPath("$[*].category", contains("C71", "G40", "G43")))
                .andExpect(jsonPath("$[*].count", contains(1, 1, 1)));
    }

    @Test
    void appointmentsByStatusListsEveryStatus() throws Exception {
        get(URL + "/appointments/by-status", bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", contains("SCHEDULED", "COMPLETED", "CANCELLED", "MISSED")))
                .andExpect(jsonPath("$[*].count", contains(2, 3, 1, 0)));
        get(URL + "/appointments/by-status?to=2026-10-31", bearer(admin))
                .andExpect(jsonPath("$[*].count", contains(0, 2, 1, 0)));
    }

    @Test
    void mriOrdersPerIsoWeek() throws Exception {
        get(URL + "/mri-orders/per-week", bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].isoWeek", contains("2026-W35", "2026-W38", "2026-W39", "2026-W45")))
                .andExpect(jsonPath("$[*].weekStart", contains("2026-08-24", "2026-09-14", "2026-09-21", "2026-11-02")))
                .andExpect(jsonPath("$[*].count", contains(1, 2, 1, 3)));
        get(URL + "/mri-orders/per-week?from=2026-09-01&to=2026-09-30", bearer(admin))
                .andExpect(jsonPath("$[*].isoWeek", contains("2026-W38", "2026-W39")))
                .andExpect(jsonPath("$[*].count", contains(2, 1)));
    }

    @Test
    void financeCountsCompletedPaymentsAndUnpaidInvoicesOnly() throws Exception {
        get(URL + "/finance", bearer(billingClerk)).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"revenue\":2200.00")))
                .andExpect(jsonPath("$.completedPayments").value(2))
                .andExpect(jsonPath("$.unpaidCount").value(2))
                .andExpect(content().string(containsString("\"unpaidAmount\":2800.00")));
        get(URL + "/finance?from=2026-11-01", bearer(admin))
                .andExpect(content().string(containsString("\"revenue\":800.00")))
                .andExpect(jsonPath("$.completedPayments").value(1))
                .andExpect(jsonPath("$.unpaidCount").value(1))
                .andExpect(content().string(containsString("\"unpaidAmount\":2500.00")));
        get(URL + "/finance?to=2026-10-31", bearer(admin))
                .andExpect(content().string(containsString("\"revenue\":1400.00")))
                .andExpect(content().string(containsString("\"unpaidAmount\":300.00")));
    }

    @Test
    void emptyRangeGivesZeros() throws Exception {
        get(URL + "/finance?from=2030-01-01", bearer(admin)).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"revenue\":0.00")))
                .andExpect(jsonPath("$.completedPayments").value(0))
                .andExpect(content().string(containsString("\"unpaidAmount\":0.00")));
        get(URL + "/patients/new-per-month?from=2030-01-01", bearer(admin)).andExpect(jsonPath("$", empty()));
        get(URL + "/appointments/by-status?from=2030-01-01", bearer(admin))
                .andExpect(jsonPath("$[*].count", contains(0, 0, 0, 0)));
    }

    @Test
    void invalidRangeReturns400() throws Exception {
        assertStandardError(get(URL + "/finance?from=2026-11-02&to=2026-11-01", bearer(admin)), 400);
        assertStandardError(get(URL + "/patients/new-per-month?from=yesterday", bearer(admin)), 400);
        // a single day is a valid range
        get(URL + "/finance?from=2026-11-06&to=2026-11-06", bearer(admin))
                .andExpect(content().string(containsString("\"revenue\":800.00")));
    }

    // ---- access (proposed rules) ----

    @Test
    void adminSeesEverySection() throws Exception {
        for (String path : new String[]{"/patients/new-per-month", "/diagnoses/by-category", "/appointments/by-status",
                "/mri-orders/per-week", "/finance"}) {
            get(URL + path, bearer(admin)).andExpect(status().isOk());
        }
    }

    @Test
    void neurologistSeesClinicalSectionsOnly() throws Exception {
        for (String path : new String[]{"/patients/new-per-month", "/diagnoses/by-category", "/appointments/by-status",
                "/mri-orders/per-week"}) {
            get(URL + path, bearer(neurologist)).andExpect(status().isOk());
            assertStandardError(get(URL + path, bearer(billingClerk)), 403);
        }
        assertStandardError(get(URL + "/finance", bearer(neurologist)), 403);
    }

    @Test
    void otherRolesAndPatientsAreDenied() throws Exception {
        for (String path : new String[]{"/patients/new-per-month", "/diagnoses/by-category", "/appointments/by-status",
                "/mri-orders/per-week", "/finance"}) {
            for (String token : new String[]{bearer(radiologist), bearer(mriTech), bearer(biomedEngineer), bearer(patientA)}) {
                assertStandardError(get(URL + path, token), 403);
            }
            assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(URL + path)), 401);
        }
    }
}
