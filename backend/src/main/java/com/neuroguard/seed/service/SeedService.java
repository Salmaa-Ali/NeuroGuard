package com.neuroguard.seed.service;

import com.neuroguard.billing.service.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Demo/test data with fixed 2026 dates, so the dashboard totals in docs/clinical-billing.md are always the
 * same. Every person record is fictional and prefixed "TEST". Written with plain SQL on purpose: it sets
 * historical timestamps (created_at, payment_date) that the JPA auditing would overwrite, and it writes
 * appointment and MRI order rows without touching the appointment/imaging modules' code.
 */
@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    /** Dev-only password for every seeded account. Never use it on a real system. */
    public static final String DEV_PASSWORD = "NeuroGuard-Dev-2026!";
    public static final String MARKER_EMAIL = "test.admin@neuroguard.local";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    public SeedService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Idempotent: if the seeded admin or the first seeded patient already exists, nothing is written.
     * Everything runs in one transaction, so a failed seed leaves no partial data to trip the check above.
     *
     * @return true if data was created, false if it was already there
     */
    @Transactional
    public boolean seed() {
        Integer existing = jdbc.queryForObject(
                "SELECT (SELECT count(*) FROM staff WHERE email = ?) + (SELECT count(*) FROM patient WHERE patient_number = 'TEST-P01')",
                Integer.class, MARKER_EMAIL);
        if (existing != null && existing > 0) {
            log.info("Seed data already present ({} exists): nothing to do", MARKER_EMAIL);
            return false;
        }
        String hash = passwordEncoder.encode(DEV_PASSWORD);

        // ---- staff: one admin and one account per permission scope
        staff("TEST Admin", MARKER_EMAIL, "ADMIN", null, hash);
        UUID neuro = staff("TEST Neurologist", "test.neurologist@neuroguard.local", "CLINICAL_STAFF", "NEUROLOGIST", hash);
        staff("TEST Radiologist", "test.radiologist@neuroguard.local", "CLINICAL_STAFF", "RADIOLOGIST", hash);
        staff("TEST MRI Technologist", "test.mritech@neuroguard.local", "CLINICAL_STAFF", "MRI_TECHNOLOGIST", hash);
        staff("TEST Biomedical Engineer", "test.biomed@neuroguard.local", "CLINICAL_STAFF", "BIOMEDICAL_ENGINEER", hash);
        staff("TEST Billing Clerk", "test.billing@neuroguard.local", "CLINICAL_STAFF", "BILLING_CLERK", hash);

        // ---- 10 patients, created Sep 3 / Oct 3 / Nov 4
        String[] created = {"2026-09-02", "2026-09-15", "2026-09-28", "2026-10-05", "2026-10-19", "2026-10-30",
                "2026-11-03", "2026-11-10", "2026-11-17", "2026-11-24"};
        Map<Integer, UUID> p = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            p.put(i, patient(i, created[i - 1], hash));
        }

        // ---- services (the report's catalogue, EGP); an existing entry with the same name is reused
        UUID consult = service("Neurology consultation", "800.00");
        UUID mri = service("Brain MRI", "2500.00");
        UUID contrast = service("MRI with contrast", "3500.00");
        UUID report = service("Report", "300.00");

        // ---- appointments: SCHEDULED 3, COMPLETED 6, CANCELLED 2, MISSED 1
        UUID a1 = appointment(p.get(1), neuro, "2026-09-05T09:00:00Z", "COMPLETED", "TEST seizure follow-up");
        UUID a2 = appointment(p.get(2), neuro, "2026-09-18T10:00:00Z", "COMPLETED", "TEST recurrent headaches");
        UUID a3 = appointment(p.get(3), neuro, "2026-10-01T11:00:00Z", "COMPLETED", "TEST numbness and fatigue");
        UUID a4 = appointment(p.get(4), neuro, "2026-10-08T09:30:00Z", "COMPLETED", "TEST post-stroke review");
        UUID a5 = appointment(p.get(5), neuro, "2026-10-22T12:00:00Z", "COMPLETED", "TEST migraine review");
        UUID a7 = appointment(p.get(7), neuro, "2026-11-05T09:00:00Z", "COMPLETED", "TEST new seizures");
        appointment(p.get(6), neuro, "2026-11-02T13:00:00Z", "CANCELLED", "TEST tremor assessment");
        appointment(p.get(10), neuro, "2026-11-26T10:00:00Z", "CANCELLED", "TEST memory complaints");
        appointment(p.get(8), neuro, "2026-11-12T14:00:00Z", "MISSED", "TEST dizziness");
        appointment(p.get(7), neuro, "2026-12-01T09:00:00Z", "SCHEDULED", "TEST seizure follow-up");
        appointment(p.get(8), neuro, "2026-12-03T09:00:00Z", "SCHEDULED", "TEST rescheduled visit");
        appointment(p.get(9), neuro, "2026-12-08T09:00:00Z", "SCHEDULED", "TEST first visit");

        // ---- MRI orders: W37 2, W39 1, W41 2, W43 1, W45 2
        mriOrder(a1, p.get(1), 1, "2026-09-08T10:00:00Z", "REPORTED", "TEST epilepsy protocol");
        mriOrder(a1, p.get(1), 2, "2026-09-10T10:00:00Z", "REPORTED", "TEST follow-up sequence");
        mriOrder(a2, p.get(2), 3, "2026-09-22T10:00:00Z", "REPORTED", "TEST rule out lesion");
        mriOrder(a3, p.get(3), 4, "2026-10-06T10:00:00Z", "ACQUIRED", "TEST demyelination workup");
        mriOrder(a4, p.get(4), 5, "2026-10-09T10:00:00Z", "REPORTED", "TEST infarct follow-up");
        mriOrder(a5, p.get(5), 6, "2026-10-23T10:00:00Z", "CANCELLED", "TEST migraine with aura");
        mriOrder(a7, p.get(7), 7, "2026-11-06T10:00:00Z", "SCHEDULED", "TEST mass lesion");
        mriOrder(a7, p.get(7), 8, "2026-11-07T10:00:00Z", "ORDERED", "TEST contrast study");

        // ---- diagnoses: G40 3, G43 2, C71 1, G20 1, G35 1, I63 1
        diagnosis(p.get(1), neuro, "G40", "2026-09-05T09:30:00Z", "TEST focal seizures");
        diagnosis(p.get(1), neuro, "G20", "2026-09-05T09:40:00Z", "TEST early resting tremor");
        diagnosis(p.get(2), neuro, "G43", "2026-09-18T10:30:00Z", "TEST migraine without aura");
        diagnosis(p.get(3), neuro, "G35", "2026-10-01T11:30:00Z", "TEST relapsing-remitting pattern");
        diagnosis(p.get(4), neuro, "G40", "2026-10-08T09:45:00Z", "TEST post-stroke seizure");
        diagnosis(p.get(4), neuro, "I63", "2026-10-08T09:50:00Z", "TEST left MCA territory");
        diagnosis(p.get(5), neuro, "G43", "2026-10-22T12:30:00Z", "TEST migraine with aura");
        diagnosis(p.get(7), neuro, "G40", "2026-11-05T09:30:00Z", "TEST new-onset seizures");
        diagnosis(p.get(7), neuro, "C71", "2026-11-05T09:45:00Z", "TEST suspected glioma");

        // ---- treatment plans (4) with prescriptions (7)
        UUID plan1 = plan(p.get(1), neuro, "TEST epilepsy management", "2026-09-05", "2027-03-05");
        prescription(plan1, neuro, "TEST Levetiracetam 500 mg, twice daily, oral");
        prescription(plan1, neuro, "TEST Folic acid 5 mg, once daily, oral");
        UUID plan2 = plan(p.get(2), neuro, "TEST migraine prevention", "2026-09-18", null);
        prescription(plan2, neuro, "TEST Sumatriptan 50 mg, as needed, max 2 per day");
        prescription(plan2, neuro, "TEST Propranolol 40 mg, twice daily, oral");
        UUID plan4 = plan(p.get(4), neuro, "TEST secondary stroke prevention", "2026-10-08", null);
        prescription(plan4, neuro, "TEST Aspirin 100 mg, once daily, oral");
        prescription(plan4, neuro, "TEST Atorvastatin 40 mg, at night, oral");
        UUID plan7 = plan(p.get(7), neuro, "TEST seizure control pending oncology review", "2026-11-05", "2027-05-05");
        prescription(plan7, neuro, "TEST Lamotrigine 25 mg, once daily, oral");

        // ---- invoices: PAID 4 (10600.00 received), UNPAID 2 (9700.00), CANCELLED 1
        UUID inv1 = invoice(p.get(1), a1, "2026-09-05", "PAID",
                List.of(line(consult, 1, null), line(mri, 1, "G40"), line(report, 1, null)));            // 3600.00
        payment(inv1, "2026-09-06T12:00:00Z");
        UUID inv2 = invoice(p.get(2), a2, "2026-09-18", "PAID", List.of(line(consult, 1, "G43")));     // 800.00
        payment(inv2, "2026-09-18T15:00:00Z");
        UUID inv3 = invoice(p.get(3), a3, "2026-10-01", "PAID",
                List.of(line(consult, 1, null), line(contrast, 1, "G35"), line(report, 1, null)));       // 4600.00
        payment(inv3, "2026-10-03T09:00:00Z");
        invoice(p.get(4), a4, "2026-10-08", "UNPAID", List.of(line(consult, 1, null), line(mri, 1, "I63"))); // 3300.00
        invoice(p.get(5), a5, "2026-10-22", "CANCELLED", List.of(line(mri, 1, null)));                  // 2500.00
        invoice(p.get(7), a7, "2026-11-05", "UNPAID",
                List.of(line(consult, 1, null), line(mri, 2, "C71"), line(report, 2, null)));           // 6400.00
        UUID inv7 = invoice(p.get(8), null, "2026-11-12", "PAID", List.of(line(consult, 2, null)));    // 1600.00
        payment(inv7, "2026-11-13T10:00:00Z");

        log.info("Seed data created: 6 staff, 10 patients, 4 services, 12 appointments, 8 MRI orders, "
                + "9 diagnoses, 4 treatment plans, 7 prescriptions, 7 invoices, 4 payments");
        return true;
    }

    // ---- helpers ----

    private record Line(UUID serviceId, int quantity, String icd10Code) {
    }

    private static Line line(UUID serviceId, int quantity, String icd10Code) {
        return new Line(serviceId, quantity, icd10Code);
    }

    private static Timestamp ts(String instant) {
        return Timestamp.from(Instant.parse(instant));
    }

    private static Timestamp startOf(String date) {
        return Timestamp.from(LocalDate.parse(date).atTime(9, 0).toInstant(ZoneOffset.UTC));
    }

    private UUID staff(String name, String email, String role, String scope, String hash) {
        return jdbc.queryForObject("INSERT INTO staff (name, email, password_hash, base_role, permission_scope) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING staff_id", UUID.class, name, email, hash, role, scope);
    }

    private UUID patient(int n, String createdOn, String hash) {
        String number = String.format(Locale.ROOT, "TEST-P%02d", n);
        return jdbc.queryForObject("INSERT INTO patient (patient_number, email, password_hash, name, date_of_birth, phone, "
                        + "neurology_history, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING patient_id",
                UUID.class, number, String.format(Locale.ROOT, "test.patient%02d@neuroguard.local", n), hash,
                String.format(Locale.ROOT, "TEST Patient %02d", n), Date.valueOf(LocalDate.of(1960 + n * 4, (n % 12) + 1, 10)),
                String.format(Locale.ROOT, "+20 100 000 00%02d", n), "TEST fictional history for demo purposes",
                startOf(createdOn), startOf(createdOn));
    }

    private UUID service(String name, String price) {
        List<UUID> found = jdbc.queryForList("SELECT service_id FROM service WHERE lower(trim(name)) = lower(?)",
                UUID.class, name);
        if (!found.isEmpty()) {
            return found.get(0);
        }
        return jdbc.queryForObject("INSERT INTO service (name, price) VALUES (?, ?::numeric) RETURNING service_id",
                UUID.class, name, price);
    }

    private UUID appointment(UUID patient, UUID staff, String at, String status, String reason) {
        return jdbc.queryForObject("INSERT INTO appointment (patient_id, staff_id, scheduled_time, status, reason) "
                + "VALUES (?, ?, ?, ?, ?) RETURNING appointment_id", UUID.class, patient, staff, ts(at), status, reason);
    }

    private void mriOrder(UUID appointment, UUID patient, int n, String createdAt, String status, String indication) {
        jdbc.update("INSERT INTO mri_order (appointment_id, patient_id, accession_number, indication, status, created_at, "
                        + "updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)", appointment, patient,
                String.format(Locale.ROOT, "TEST-ACC-%04d", n), indication, status, ts(createdAt), ts(createdAt));
    }

    private void diagnosis(UUID patient, UUID staff, String code, String at, String notes) {
        jdbc.update("INSERT INTO diagnosis (patient_id, staff_id, icd10_code, diagnosed_at, notes, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)", patient, staff, code, ts(at), notes, ts(at), ts(at));
    }

    private UUID plan(UUID patient, UUID staff, String description, String start, String end) {
        return jdbc.queryForObject("INSERT INTO treatment_plan (patient_id, staff_id, description, start_date, end_date) "
                        + "VALUES (?, ?, ?, ?, ?) RETURNING plan_id", UUID.class, patient, staff, description,
                Date.valueOf(start), end == null ? null : Date.valueOf(end));
    }

    private void prescription(UUID plan, UUID staff, String details) {
        jdbc.update("INSERT INTO prescription (plan_id, staff_id, medication_details) VALUES (?, ?, ?)",
                plan, staff, details);
    }

    /** Same arithmetic as InvoiceService: unit price snapshot, line = unit x quantity, total = sum. */
    private UUID invoice(UUID patient, UUID appointment, String issueDate, String status, List<Line> lines) {
        BigDecimal total = Money.of(BigDecimal.ZERO);
        BigDecimal[] unitPrices = new BigDecimal[lines.size()];
        for (int i = 0; i < lines.size(); i++) {
            unitPrices[i] = Money.of(jdbc.queryForObject("SELECT price FROM service WHERE service_id = ?",
                    BigDecimal.class, lines.get(i).serviceId()));
            total = total.add(Money.of(unitPrices[i].multiply(BigDecimal.valueOf(lines.get(i).quantity()))));
        }
        UUID invoice = jdbc.queryForObject("INSERT INTO invoice (patient_id, appointment_id, total_amount, status, "
                        + "issue_date) VALUES (?, ?, ?, ?, ?) RETURNING invoice_id", UUID.class, patient, appointment, total,
                status, Date.valueOf(issueDate));
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            jdbc.update("INSERT INTO invoice_item (invoice_id, service_id, icd10_code, quantity, unit_price, line_total) "
                            + "VALUES (?, ?, ?, ?, ?, ?)", invoice, l.serviceId(), l.icd10Code(), l.quantity(), unitPrices[i],
                    Money.of(unitPrices[i].multiply(BigDecimal.valueOf(l.quantity()))));
        }
        return invoice;
    }

    /** Simulated, COMPLETED, for the invoice's full total. */
    private void payment(UUID invoice, String at) {
        jdbc.update("INSERT INTO payment (invoice_id, amount_paid, status, payment_date) "
                + "SELECT invoice_id, total_amount, 'COMPLETED', ? FROM invoice WHERE invoice_id = ?", ts(at), invoice);
    }
}
