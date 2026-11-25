package com.neuroguard.billing;

import com.neuroguard.billing.entity.ServiceItem;
import com.neuroguard.billing.repository.ServiceItemRepository;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Matrix: Billing clerk RW, Admin R, patient own R. Amounts are always computed by the server. */
class InvoiceTest extends AbstractIntegrationTest {

    private static final String URL = "/api/invoices";

    @Autowired
    private ServiceItemRepository serviceRepository;

    private ServiceItem consult;   // 800.00
    private ServiceItem mri;       // 2500.00
    private ServiceItem report;    // 300.00

    @BeforeEach
    void services() {
        jdbc.execute("TRUNCATE TABLE service CASCADE");
        consult = service("TEST Neurology consultation", "800.00");
        mri = service("TEST Brain MRI", "2500.00");
        report = service("TEST Report", "300.00");
    }

    private ServiceItem service(String name, String price) {
        ServiceItem s = new ServiceItem();
        s.setName(name);
        s.setPrice(new BigDecimal(price));
        return serviceRepository.saveAndFlush(s);
    }

    private static Map<String, Object> item(ServiceItem s, Integer quantity) {
        Map<String, Object> item = new HashMap<>();
        item.put("serviceId", s.getId().toString());
        item.put("quantity", quantity);
        return item;
    }

    private static Map<String, Object> invoice(Patient p, List<Map<String, Object>> items) {
        Map<String, Object> body = new HashMap<>();
        body.put("patientId", p.getId().toString());
        body.put("items", items);
        return body;
    }

    private String create(Patient p, List<Map<String, Object>> items) throws Exception {
        String json = postJson(URL, bearer(billingClerk), invoice(p, items))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private BigDecimal dbTotal(String invoiceId) {
        return jdbc.queryForObject("SELECT total_amount FROM invoice WHERE invoice_id = ?::uuid",
                BigDecimal.class, invoiceId);
    }

    private UUID appointment(Patient p) {
        return jdbc.queryForObject("INSERT INTO appointment (patient_id, staff_id, scheduled_time) VALUES (?, ?, ?) "
                + "RETURNING appointment_id", UUID.class, p.getId(), neurologist.getId(), Timestamp.from(Instant.now()));
    }

    private ResultActions cancel(String id, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post(URL + "/" + id + "/cancel").header("Authorization", token));
    }

    // ---- arithmetic ----

    @Test
    void totalsAreComputedFromCataloguePricesForSeveralItemsAndQuantities() throws Exception {
        // 1 x 800.00 + 2 x 2500.00 + 3 x 300.00 = 800.00 + 5000.00 + 900.00 = 6700.00
        ResultActions result = postJson(URL, bearer(billingClerk),
                invoice(patientA, List.of(item(consult, 1), item(mri, 2), item(report, 3))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("UNPAID"))
                .andExpect(jsonPath("$.patientId").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[*].serviceName",
                        containsInAnyOrder("TEST Neurology consultation", "TEST Brain MRI", "TEST Report")))
                .andExpect(content().string(containsString("\"totalAmount\":6700.00")))
                .andExpect(content().string(containsString("\"unitPrice\":2500.00,\"lineTotal\":5000.00")))
                .andExpect(content().string(containsString("\"unitPrice\":300.00,\"lineTotal\":900.00")));
        String id = objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();

        assertEquals(new BigDecimal("6700.00"), dbTotal(id));
        assertEquals(new BigDecimal("6700.00"), jdbc.queryForObject(
                "SELECT sum(line_total) FROM invoice_item WHERE invoice_id = ?::uuid", BigDecimal.class, id));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'INVOICE_CREATED' AND actor_id = ?
                  AND entity_type = 'INVOICE' AND entity_id = ?::uuid
                """, Integer.class, billingClerk.getId(), id));
    }

    @Test
    void centsAreExact() throws Exception {
        ServiceItem odd = service("TEST Odd price", "333.33");
        ServiceItem cent = service("TEST One cent", "0.01");
        // 3 x 333.33 = 999.99; 7 x 0.01 = 0.07; total 1000.06 (would drift with double arithmetic)
        String id = create(patientA, List.of(item(odd, 3), item(cent, 7)));
        assertEquals(new BigDecimal("1000.06"), dbTotal(id));
        assertEquals(List.of(new BigDecimal("0.07"), new BigDecimal("999.99")), jdbc.queryForList(
                "SELECT line_total FROM invoice_item WHERE invoice_id = ?::uuid ORDER BY line_total",
                BigDecimal.class, id));
    }

    @Test
    void laterPriceChangeDoesNotAlterOldInvoice() throws Exception {
        String oldInvoice = create(patientA, List.of(item(consult, 2))); // 2 x 800.00 = 1600.00

        mockMvc.perform(MockMvcRequestBuilders.put("/api/services/" + consult.getId())
                        .header("Authorization", bearer(billingClerk)).contentType("application/json")
                        .content("{\"name\":\"TEST Neurology consultation\",\"price\":950.00}"))
                .andExpect(status().isOk());

        get(URL + "/" + oldInvoice, bearer(billingClerk)).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"unitPrice\":800.00,\"lineTotal\":1600.00")))
                .andExpect(content().string(containsString("\"totalAmount\":1600.00")));
        assertEquals(new BigDecimal("1600.00"), dbTotal(oldInvoice));

        String newInvoice = create(patientA, List.of(item(consult, 2))); // 2 x 950.00 = 1900.00
        assertEquals(new BigDecimal("1900.00"), dbTotal(newInvoice));
    }

    @Test
    void clientSuppliedPricesAndTotalsAreIgnored() throws Exception {
        Map<String, Object> line = item(mri, 1);
        line.put("unitPrice", 1);
        line.put("lineTotal", 1);
        Map<String, Object> body = invoice(patientA, List.of(line));
        body.put("totalAmount", 1);
        body.put("status", "PAID");
        String json = postJson(URL, bearer(billingClerk), body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("UNPAID"))
                .andReturn().getResponse().getContentAsString();
        assertEquals(new BigDecimal("2500.00"), dbTotal(objectMapper.readTree(json).get("id").asText()));
    }

    @Test
    void amountsAboveNumeric10x2Return400AndNothingIsSaved() throws Exception {
        ServiceItem max = service("TEST Max price", "99999999.99");
        ServiceItem big = service("TEST Big", "60000000.00");

        // exactly the maximum is allowed
        String ok = create(patientA, List.of(item(max, 1)));
        assertEquals(new BigDecimal("99999999.99"), dbTotal(ok));

        // one line above the maximum: 2 x 99,999,999.99
        ResultActions line = postJson(URL, bearer(billingClerk), invoice(patientA, List.of(item(max, 2))));
        assertStandardError(line, 400);
        line.andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"))
                .andExpect(jsonPath("$.message", containsString("exceeds the maximum of 99999999.99")));

        // every line fits but the total does not: 60,000,000.00 + 60,000,000.00
        ResultActions total = postJson(URL, bearer(billingClerk), invoice(patientA, List.of(item(big, 1), item(big, 1))));
        assertStandardError(total, 400);
        total.andExpect(jsonPath("$.fieldErrors[0].field").value("items"))
                .andExpect(jsonPath("$.message", containsString("Invoice total 120000000.00")));

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM invoice", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM invoice_item", Integer.class));
    }

    // ---- validation ----

    @Test
    void itemsAreRequiredAndQuantityMustBeAtLeastOne() throws Exception {
        ResultActions empty = postJson(URL, bearer(billingClerk), invoice(patientA, List.of()));
        assertStandardError(empty, 400);
        empty.andExpect(jsonPath("$.fieldErrors[0].field").value("items"));

        Map<String, Object> noItems = invoice(patientA, List.of());
        noItems.remove("items");
        assertStandardError(postJson(URL, bearer(billingClerk), noItems), 400);

        for (Integer quantity : new Integer[]{0, -1, null}) {
            ResultActions bad = postJson(URL, bearer(billingClerk), invoice(patientA, List.of(item(consult, quantity))));
            assertStandardError(bad, 400);
            bad.andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].quantity"));
        }
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM invoice", Integer.class));
    }

    @Test
    void unknownServiceOrPatientReturns400() throws Exception {
        Map<String, Object> ghost = new HashMap<>();
        ghost.put("serviceId", UUID.randomUUID().toString());
        ghost.put("quantity", 1);
        ResultActions service = postJson(URL, bearer(billingClerk), invoice(patientA, List.of(item(consult, 1), ghost)));
        assertStandardError(service, 400);
        service.andExpect(jsonPath("$.fieldErrors[0].field").value("items[1].serviceId"));

        Map<String, Object> body = invoice(patientA, List.of(item(consult, 1)));
        body.put("patientId", UUID.randomUUID().toString());
        ResultActions patient = postJson(URL, bearer(billingClerk), body);
        assertStandardError(patient, 400);
        patient.andExpect(jsonPath("$.fieldErrors[0].field").value("patientId"));

        body.remove("patientId");
        assertStandardError(postJson(URL, bearer(billingClerk), body), 400);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM invoice", Integer.class));
    }

    @Test
    void icd10CodeIsOptionalButMustExist() throws Exception {
        Map<String, Object> coded = item(mri, 1);
        coded.put("icd10Code", "g40");
        String id = create(patientA, List.of(coded, item(report, 1)));
        assertEquals(List.of("G40"), jdbc.queryForList(
                "SELECT icd10_code FROM invoice_item WHERE invoice_id = ?::uuid AND icd10_code IS NOT NULL",
                String.class, id));

        Map<String, Object> unknown = item(mri, 1);
        unknown.put("icd10Code", "ZZZ99");
        ResultActions result = postJson(URL, bearer(billingClerk), invoice(patientA, List.of(unknown)));
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.fieldErrors[0].field").value("items[0].icd10Code"));
    }

    @Test
    void appointmentMustBelongToTheSamePatient() throws Exception {
        UUID own = appointment(patientA);
        UUID other = appointment(patientB);

        Map<String, Object> body = invoice(patientA, List.of(item(consult, 1)));
        body.put("appointmentId", own.toString());
        postJson(URL, bearer(billingClerk), body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.appointmentId").value(own.toString()));

        body.put("appointmentId", other.toString());
        ResultActions mismatch = postJson(URL, bearer(billingClerk), body);
        assertStandardError(mismatch, 400);
        mismatch.andExpect(jsonPath("$.fieldErrors[0].field").value("appointmentId"))
                .andExpect(jsonPath("$.message").value("The appointment belongs to a different patient"));

        body.put("appointmentId", UUID.randomUUID().toString());
        assertStandardError(postJson(URL, bearer(billingClerk), body), 400);
    }

    @Test
    void onlyBillingClerkMayCreate() throws Exception {
        Map<String, Object> body = invoice(patientA, List.of(item(consult, 1)));
        for (String token : new String[]{bearer(admin), bearer(neurologist), bearer(radiologist), bearer(mriTech),
                bearer(biomedEngineer), bearer(patientA)}) {
            assertStandardError(postJson(URL, token, body), 403);
        }
        assertStandardError(postJson(URL, body), 401);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM invoice", Integer.class));
    }

    // ---- status machine ----

    @Test
    void clerkCancelsUnpaidInvoiceAndItIsAudited() throws Exception {
        String id = create(patientA, List.of(item(consult, 1)));
        cancel(id, bearer(billingClerk)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.items.length()").value(1));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'INVOICE_CANCELLED' AND actor_id = ? AND entity_id = ?::uuid
                """, Integer.class, billingClerk.getId(), id));
    }

    @Test
    void cancelledAndPaidAreFinal() throws Exception {
        String cancelled = create(patientA, List.of(item(consult, 1)));
        cancel(cancelled, bearer(billingClerk)).andExpect(status().isOk());
        ResultActions again = cancel(cancelled, bearer(billingClerk));
        assertStandardError(again, 409);
        again.andExpect(jsonPath("$.message", containsString("CANCELLED")));

        String paid = create(patientA, List.of(item(consult, 1)));
        jdbc.update("UPDATE invoice SET status = 'PAID' WHERE invoice_id = ?::uuid", paid);
        ResultActions paidCancel = cancel(paid, bearer(billingClerk));
        assertStandardError(paidCancel, 409);
        paidCancel.andExpect(jsonPath("$.message", containsString("PAID")));
        assertEquals("PAID", jdbc.queryForObject("SELECT status FROM invoice WHERE invoice_id = ?::uuid",
                String.class, paid));

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = 'INVOICE_CANCELLED'",
                Integer.class));
    }

    @Test
    void cancelRulesForRolesAndUnknownIds() throws Exception {
        String id = create(patientA, List.of(item(consult, 1)));
        for (String token : new String[]{bearer(admin), bearer(neurologist), bearer(patientA)}) {
            assertStandardError(cancel(id, token), 403);
        }
        assertStandardError(cancel(UUID.randomUUID().toString(), bearer(billingClerk)), 404);
        assertEquals("UNPAID", jdbc.queryForObject("SELECT status FROM invoice WHERE invoice_id = ?::uuid",
                String.class, id));
    }

    // ---- lists and reads ----

    @Test
    void allInvoicesWithStatusFilterForClerkAndAdmin() throws Exception {
        create(patientA, List.of(item(consult, 1)));
        create(patientB, List.of(item(mri, 1)));
        String cancelled = create(patientA, List.of(item(report, 1)));
        cancel(cancelled, bearer(billingClerk)).andExpect(status().isOk());

        for (String token : new String[]{bearer(billingClerk), bearer(admin)}) {
            get(URL, token).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
            get(URL + "?status=UNPAID", token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[*].status", containsInAnyOrder("UNPAID", "UNPAID")));
            get(URL + "?status=CANCELLED", token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(cancelled)));
            get(URL + "?status=PAID", token).andExpect(jsonPath("$.totalElements").value(0));
        }
        assertStandardError(get(URL + "?status=OVERDUE", bearer(billingClerk)), 400);

        for (String token : new String[]{bearer(patientA), bearer(neurologist), bearer(radiologist), bearer(mriTech),
                bearer(biomedEngineer)}) {
            assertStandardError(get(URL, token), 403);
        }
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(URL)), 401);
    }

    @Test
    void invoicesByPatientFollowTheMatrix() throws Exception {
        String a1 = create(patientA, List.of(item(consult, 1)));
        String a2 = create(patientA, List.of(item(mri, 1)));
        create(patientB, List.of(item(report, 1)));
        String urlA = "/api/patients/" + patientA.getId() + "/invoices";

        for (String token : new String[]{bearer(billingClerk), bearer(admin), bearer(patientA)}) {
            get(urlA, token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].id", containsInAnyOrder(a1, a2)));
        }
        get(urlA + "?status=CANCELLED", bearer(patientA)).andExpect(jsonPath("$.totalElements").value(0));

        for (String token : new String[]{bearer(patientB), bearer(neurologist), bearer(radiologist), bearer(mriTech),
                bearer(biomedEngineer)}) {
            assertStandardError(get(urlA, token), 403);
        }
        String unknown = "/api/patients/" + UUID.randomUUID() + "/invoices";
        assertStandardError(get(unknown, bearer(billingClerk)), 404);
        assertStandardError(get(unknown, bearer(patientA)), 403);
    }

    @Test
    void getOneChecksOwnershipInTheService() throws Exception {
        String id = create(patientA, List.of(item(consult, 1), item(report, 2)));
        String url = URL + "/" + id;

        for (String token : new String[]{bearer(billingClerk), bearer(admin), bearer(patientA)}) {
            get(url, token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(content().string(containsString("\"totalAmount\":1400.00")));
        }
        assertStandardError(get(url, bearer(patientB)), 403);
        for (String token : new String[]{bearer(neurologist), bearer(radiologist), bearer(mriTech), bearer(biomedEngineer)}) {
            assertStandardError(get(url, token), 403);
        }

        String unknown = URL + "/" + UUID.randomUUID();
        assertStandardError(get(unknown, bearer(billingClerk)), 404);
        assertStandardError(get(unknown, bearer(patientA)), 403);
    }

    @Test
    void listIsPaged() throws Exception {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ids.add(create(patientA, List.of(item(consult, 1))));
        }
        get(URL + "?size=2&page=0", bearer(admin)).andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
        get(URL + "?size=2&page=1", bearer(admin)).andExpect(jsonPath("$.content.length()").value(1));
        // newest first: the last created invoice is on the first page
        get(URL + "?size=1&page=0", bearer(admin)).andExpect(jsonPath("$.content[0].id").value(ids.get(2)));
    }
}
