package com.neuroguard.billing;

import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Billing clerk RW; every other staff role R; patients none. */
class ServiceCatalogueTest extends AbstractIntegrationTest {

    private static final String URL = "/api/services";

    @BeforeEach
    void cleanServices() {
        // service is not covered by the fixture TRUNCATE
        jdbc.execute("TRUNCATE TABLE service CASCADE");
    }

    private static Map<String, Object> service(String name, Object price) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("price", price);
        return body;
    }

    private String create(String name, String price) throws Exception {
        String json = postJson(URL, bearer(billingClerk), service(name, new BigDecimal(price)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private ResultActions put(String id, String token, Object body) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.put(URL + "/" + id)
                .header("Authorization", token).contentType("application/json")
                .content(objectMapper.writeValueAsString(body)));
    }

    // ---- create ----

    @Test
    void clerkCreatesServiceWithTwoDecimalPriceAndItIsAudited() throws Exception {
        ResultActions result = postJson(URL, bearer(billingClerk), service("  TEST Neurology consultation ", 800))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(URL + "/")))
                .andExpect(jsonPath("$.name").value("TEST Neurology consultation"))
                .andExpect(content().string(containsString("\"price\":800.00")));
        String id = objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();
        result.andExpect(header().string("Location", endsWith(URL + "/" + id)));

        assertEquals(new BigDecimal("800.00"),
                jdbc.queryForObject("SELECT price FROM service WHERE service_id = ?::uuid", BigDecimal.class, id));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'SERVICE_CREATED' AND actor_id = ?
                  AND entity_type = 'SERVICE' AND entity_id = ?::uuid
                """, Integer.class, billingClerk.getId(), id));
    }

    @Test
    void zeroPriceIsAllowed() throws Exception {
        postJson(URL, bearer(billingClerk), service("TEST Free follow-up", 0))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString("\"price\":0.00")));
    }

    @Test
    void duplicateNameReturns409IgnoringCaseAndSpaces() throws Exception {
        create("TEST Brain MRI", "2500.00");
        assertStandardError(postJson(URL, bearer(billingClerk), service("TEST Brain MRI", 1)), 409);
        assertStandardError(postJson(URL, bearer(billingClerk), service("  test brain mri ", 1)), 409);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM service", Integer.class));
    }

    @Test
    void invalidPriceOrNameReturns400() throws Exception {
        for (Object price : new Object[]{-0.01, 12.345, new BigDecimal("100000000.00"), null}) {
            ResultActions result = postJson(URL, bearer(billingClerk), service("TEST x", price));
            assertStandardError(result, 400);
            result.andExpect(jsonPath("$.fieldErrors[0].field").value("price"));
        }
        ResultActions blank = postJson(URL, bearer(billingClerk), service(" ", 10));
        assertStandardError(blank, 400);
        blank.andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
        assertStandardError(postJson(URL, bearer(billingClerk), service("TEST x", "not-a-number")), 400);
        // the largest allowed price fits NUMERIC(10,2)
        postJson(URL, bearer(billingClerk), service("TEST max", new BigDecimal("99999999.99")))
                .andExpect(status().isCreated());
    }

    // ---- update ----

    @Test
    void clerkUpdatesNameAndPriceAndItIsAudited() throws Exception {
        String id = create("TEST Report", "300.00");
        put(id, bearer(billingClerk), service("TEST Radiology report", new BigDecimal("350.5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("TEST Radiology report"))
                .andExpect(content().string(containsString("\"price\":350.50")));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'SERVICE_UPDATED' AND actor_id = ? AND entity_id = ?::uuid
                """, Integer.class, billingClerk.getId(), id));
    }

    @Test
    void updateDuplicateRules() throws Exception {
        String mri = create("TEST Brain MRI", "2500.00");
        create("TEST MRI with contrast", "3500.00");
        // renaming to another entry's name conflicts
        assertStandardError(put(mri, bearer(billingClerk), service("test mri WITH contrast", 1)), 409);
        // keeping its own name (even with different case) is fine
        put(mri, bearer(billingClerk), service("TEST BRAIN MRI", 2600)).andExpect(status().isOk());
    }

    @Test
    void updateUnknownOrInvalid() throws Exception {
        assertStandardError(put(UUID.randomUUID().toString(), bearer(billingClerk), service("TEST x", 1)), 404);
        String id = create("TEST x", "1.00");
        assertStandardError(put(id, bearer(billingClerk), service("TEST x", -5)), 400);
    }

    @Test
    void onlyBillingClerkMayWrite() throws Exception {
        String id = create("TEST Brain MRI", "2500.00");
        for (String token : new String[]{bearer(admin), bearer(neurologist), bearer(radiologist), bearer(mriTech),
                bearer(biomedEngineer), bearer(patientA)}) {
            assertStandardError(postJson(URL, token, service("TEST other", 1)), 403);
            assertStandardError(put(id, token, service("TEST other", 1)), 403);
        }
        assertStandardError(postJson(URL, service("TEST other", 1)), 401);
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM service", Integer.class));
    }

    // ---- read ----

    @Test
    void anyStaffCanReadListSortedByNameAndOne() throws Exception {
        String report = create("TEST Report", "300.00");
        create("TEST Brain MRI", "2500.00");
        create("TEST Neurology consultation", "800.00");

        for (String token : new String[]{bearer(admin), bearer(neurologist), bearer(radiologist), bearer(mriTech),
                bearer(biomedEngineer), bearer(billingClerk)}) {
            get(URL, token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[*].name",
                            contains("TEST Brain MRI", "TEST Neurology consultation", "TEST Report")));
            get(URL + "/" + report, token).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("TEST Report"));
        }
        get(URL + "?size=2&page=1", bearer(neurologist)).andExpect(jsonPath("$.content[*].name", contains("TEST Report")))
                .andExpect(jsonPath("$.totalElements").value(3));
        assertStandardError(get(URL + "/" + UUID.randomUUID(), bearer(neurologist)), 404);
    }

    @Test
    void patientsAndAnonymousCannotRead() throws Exception {
        String id = create("TEST Report", "300.00");
        assertStandardError(get(URL, bearer(patientA)), 403);
        assertStandardError(get(URL + "/" + id, bearer(patientA)), 403);
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(URL)), 401);
    }
}
