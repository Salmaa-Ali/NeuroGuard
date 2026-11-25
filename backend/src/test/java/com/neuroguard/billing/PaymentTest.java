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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Simulated payment: billing clerk or the patient (own invoice); full total; UNPAID -> PAID only. */
class PaymentTest extends AbstractIntegrationTest {

    @Autowired
    private ServiceItemRepository serviceRepository;

    private ServiceItem consult; // 800.00
    private ServiceItem report;  // 300.00

    @BeforeEach
    void services() {
        jdbc.execute("TRUNCATE TABLE service CASCADE");
        consult = service("TEST Neurology consultation", "800.00");
        report = service("TEST Report", "300.00");
    }

    private ServiceItem service(String name, String price) {
        ServiceItem s = new ServiceItem();
        s.setName(name);
        s.setPrice(new BigDecimal(price));
        return serviceRepository.saveAndFlush(s);
    }

    /** 1 x 800.00 + 2 x 300.00 = 1400.00 */
    private String invoice(Patient p) throws Exception {
        Map<String, Object> body = Map.of("patientId", p.getId().toString(), "items", List.of(
                Map.of("serviceId", consult.getId().toString(), "quantity", 1),
                Map.of("serviceId", report.getId().toString(), "quantity", 2)));
        String json = postJson("/api/invoices", bearer(billingClerk), body)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private ResultActions pay(String invoiceId, String token) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices/" + invoiceId + "/pay")
                .header("Authorization", token));
    }

    private String invoiceStatus(String id) {
        return jdbc.queryForObject("SELECT status FROM invoice WHERE invoice_id = ?::uuid", String.class, id);
    }

    private int payments() {
        return jdbc.queryForObject("SELECT count(*) FROM payment", Integer.class);
    }

    // ---- paying ----

    @Test
    void patientPaysOwnInvoiceInFull() throws Exception {
        String id = invoice(patientA);
        String json = pay(id, bearer(patientA)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(id))
                .andExpect(jsonPath("$.patientId").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(content().string(containsString("\"amountPaid\":1400.00")))
                .andReturn().getResponse().getContentAsString();
        String paymentId = objectMapper.readTree(json).get("id").asText();

        assertEquals("PAID", invoiceStatus(id));
        assertEquals(new BigDecimal("1400.00"), jdbc.queryForObject(
                "SELECT amount_paid FROM payment WHERE payment_id = ?::uuid AND status = 'COMPLETED'",
                BigDecimal.class, paymentId));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'PAYMENT_COMPLETED' AND actor_type = 'PATIENT'
                  AND actor_id = ? AND entity_type = 'PAYMENT' AND entity_id = ?::uuid
                """, Integer.class, patientA.getId(), paymentId));

        // the invoice now shows its payment
        get("/api/invoices/" + id, bearer(patientA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.payment.id").value(paymentId))
                .andExpect(jsonPath("$.payment.status").value("COMPLETED"));
    }

    @Test
    void billingClerkCanPayAnyInvoice() throws Exception {
        String id = invoice(patientB);
        pay(id, bearer(billingClerk)).andExpect(status().isCreated());
        assertEquals("PAID", invoiceStatus(id));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'PAYMENT_COMPLETED' AND actor_type = 'STAFF' AND actor_id = ?
                """, Integer.class, billingClerk.getId()));
    }

    @Test
    void paidAndCancelledInvoicesCannotBePaid() throws Exception {
        String paid = invoice(patientA);
        pay(paid, bearer(patientA)).andExpect(status().isCreated());
        ResultActions twice = pay(paid, bearer(patientA));
        assertStandardError(twice, 409);
        twice.andExpect(jsonPath("$.message", containsString("PAID")));
        // and a paid invoice cannot be cancelled afterwards
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices/" + paid + "/cancel")
                .header("Authorization", bearer(billingClerk))), 409);

        String cancelled = invoice(patientA);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices/" + cancelled + "/cancel")
                .header("Authorization", bearer(billingClerk))).andExpect(status().isOk());
        ResultActions cancelledPay = pay(cancelled, bearer(billingClerk));
        assertStandardError(cancelledPay, 409);
        cancelledPay.andExpect(jsonPath("$.message", containsString("CANCELLED")));

        assertEquals(1, payments());
        assertEquals("CANCELLED", invoiceStatus(cancelled));
    }

    @Test
    void zeroTotalInvoiceHasNothingToPay() throws Exception {
        ServiceItem free = service("TEST Free follow-up", "0.00");
        String json = postJson("/api/invoices", bearer(billingClerk), Map.of("patientId", patientA.getId().toString(),
                "items", List.of(Map.of("serviceId", free.getId().toString(), "quantity", 1))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        assertStandardError(pay(id, bearer(patientA)), 409);
        assertEquals("UNPAID", invoiceStatus(id));
        assertEquals(0, payments());
    }

    // ---- access ----

    @Test
    void anotherPatientsInvoiceReturns403AndNothingChanges() throws Exception {
        String id = invoice(patientA);
        assertStandardError(pay(id, bearer(patientB)), 403);
        assertEquals("UNPAID", invoiceStatus(id));
        assertEquals(0, payments());

        // another patient also gets 403 (not 409) for an already paid invoice: no status leak
        pay(id, bearer(patientA)).andExpect(status().isCreated());
        assertStandardError(pay(id, bearer(patientB)), 403);
    }

    @Test
    void unknownInvoiceIs404ForClerkAnd403ForPatients() throws Exception {
        String unknown = UUID.randomUUID().toString();
        assertStandardError(pay(unknown, bearer(billingClerk)), 404);
        assertStandardError(pay(unknown, bearer(patientA)), 403);
    }

    @Test
    void otherRolesCannotPay() throws Exception {
        String id = invoice(patientA);
        for (String token : new String[]{bearer(admin), bearer(neurologist), bearer(radiologist), bearer(mriTech),
                bearer(biomedEngineer)}) {
            assertStandardError(pay(id, token), 403);
        }
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices/" + id + "/pay")), 401);
        assertEquals("UNPAID", invoiceStatus(id));
        assertEquals(0, payments());
    }

    // ---- no card data ----

    @Test
    void anyRequestBodyIsRejectedSoCardDataCannotBeSent() throws Exception {
        String id = invoice(patientA);
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.post("/api/invoices/" + id + "/pay")
                .header("Authorization", bearer(patientA)).contentType("application/json")
                .content("{\"cardNumber\":\"4111111111111111\",\"cvv\":\"123\",\"expiry\":\"12/30\"}"));
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.message", containsString("no card or account details")));
        assertEquals("UNPAID", invoiceStatus(id));
        assertEquals(0, payments());
    }

    @Test
    void paymentTableHasNoCardColumns() {
        Set<String> columns = Set.copyOf(jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'payment'", String.class));
        assertEquals(Set.of("payment_id", "invoice_id", "amount_paid", "payment_date", "status", "created_at",
                "updated_at"), columns);
    }

    // ---- list ----

    @Test
    void paymentsListForClerkAndAdminNewestFirst() throws Exception {
        String first = invoice(patientA);
        String second = invoice(patientB);
        pay(first, bearer(patientA)).andExpect(status().isCreated());
        pay(second, bearer(billingClerk)).andExpect(status().isCreated());

        for (String token : new String[]{bearer(billingClerk), bearer(admin)}) {
            get("/api/payments", token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.content[*].invoiceId", contains(second, first)))
                    .andExpect(jsonPath("$.content[1].patientId").value(patientA.getId().toString()));
        }
        get("/api/payments?size=1", bearer(admin)).andExpect(jsonPath("$.totalPages").value(2));

        for (String token : new String[]{bearer(patientA), bearer(neurologist), bearer(radiologist), bearer(mriTech),
                bearer(biomedEngineer)}) {
            assertStandardError(get("/api/payments", token), 403);
        }
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get("/api/payments")), 401);
    }
}
