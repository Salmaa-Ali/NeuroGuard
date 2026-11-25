package com.neuroguard.clinical;

import com.neuroguard.patient.entity.Patient;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Matrix: Neurologist RW, Radiologist R, patient own R; Admin, MRI tech, biomed, billing no access. */
class TreatmentPlanTest extends AbstractIntegrationTest {

    static String plansUrl(Patient p) {
        return "/api/patients/" + p.getId() + "/treatment-plans";
    }

    static Map<String, Object> plan(String description, String start, String end) {
        Map<String, Object> body = new HashMap<>();
        body.put("description", description);
        body.put("startDate", start);
        body.put("endDate", end);
        return body;
    }

    private String createPlan(Patient p, String start, String end) throws Exception {
        String json = postJson(plansUrl(p), bearer(neurologist), plan("TEST plan from " + start, start, end))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private void prescribe(String planId, String details) throws Exception {
        postJson("/api/treatment-plans/" + planId + "/prescriptions", bearer(neurologist),
                Map.of("medicationDetails", details)).andExpect(status().isCreated());
    }

    // ---- create ----

    @Test
    void neurologistCreatesPlanAndItIsAudited() throws Exception {
        Map<String, Object> body = plan("  TEST levetiracetam titration  ", "2026-11-01", "2027-02-01");
        body.put("staffId", radiologist.getId().toString()); // ignored: author comes from the token

        ResultActions result = postJson(plansUrl(patientA), bearer(neurologist), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.description").value("TEST levetiracetam titration"))
                .andExpect(jsonPath("$.startDate").value("2026-11-01"))
                .andExpect(jsonPath("$.endDate").value("2027-02-01"))
                .andExpect(jsonPath("$.staffId").value(neurologist.getId().toString()))
                .andExpect(jsonPath("$.staffName").value(neurologist.getName()))
                .andExpect(jsonPath("$.prescriptions").isEmpty());
        assertNoPasswordHash(result);

        String id = objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asText();
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'TREATMENT_PLAN_CREATED' AND actor_id = ?
                  AND entity_type = 'TREATMENT_PLAN' AND entity_id = ?::uuid
                """, Integer.class, neurologist.getId(), id));
    }

    @Test
    void endDateBeforeStartDateReturns400WithClearMessage() throws Exception {
        ResultActions result = postJson(plansUrl(patientA), bearer(neurologist),
                plan("TEST plan", "2026-11-10", "2026-11-09"));
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.message").value("endDate must be on or after startDate"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("endDate"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM treatment_plan", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = 'TREATMENT_PLAN_CREATED'",
                Integer.class));
    }

    @Test
    void sameDayAndOpenEndedPlansAreAllowed() throws Exception {
        postJson(plansUrl(patientA), bearer(neurologist), plan("TEST one day", "2026-11-10", "2026-11-10"))
                .andExpect(status().isCreated());
        postJson(plansUrl(patientA), bearer(neurologist), plan("TEST open", "2026-11-10", null))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.endDate").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        ResultActions result = postJson(plansUrl(patientA), bearer(neurologist), plan(" ", null, null));
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.fieldErrors.length()").value(2));
        assertStandardError(postJson(plansUrl(patientA), bearer(neurologist),
                plan("TEST", "not-a-date", null)), 400);
    }

    @Test
    void unknownPatientReturns404() throws Exception {
        assertStandardError(postJson("/api/patients/" + UUID.randomUUID() + "/treatment-plans",
                bearer(neurologist), plan("TEST", "2026-11-01", null)), 404);
    }

    @Test
    void onlyNeurologistMayCreate() throws Exception {
        Map<String, Object> body = plan("TEST", "2026-11-01", null);
        for (String token : new String[]{bearer(radiologist), bearer(admin), bearer(mriTech),
                bearer(biomedEngineer), bearer(billingClerk), bearer(patientA)}) {
            assertStandardError(postJson(plansUrl(patientA), token, body), 403);
        }
        assertStandardError(postJson(plansUrl(patientA), body), 401);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM treatment_plan", Integer.class));
    }

    // ---- list ----

    @Test
    void listIsNewestFirstAndIncludesPrescriptionsInOrder() throws Exception {
        String older = createPlan(patientA, "2026-09-01", "2026-10-01");
        String newer = createPlan(patientA, "2026-11-01", null);
        createPlan(patientB, "2026-11-15", null);
        prescribe(newer, "TEST Levetiracetam 500 mg twice daily");
        prescribe(newer, "TEST Vitamin D 1000 IU daily");
        prescribe(older, "TEST Sumatriptan 50 mg as needed");

        get(plansUrl(patientA), bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].id", contains(newer, older)))
                .andExpect(jsonPath("$.content[0].prescriptions[*].medicationDetails",
                        contains("TEST Levetiracetam 500 mg twice daily", "TEST Vitamin D 1000 IU daily")))
                .andExpect(jsonPath("$.content[0].prescriptions[0].planId").value(newer))
                .andExpect(jsonPath("$.content[1].prescriptions[*].medicationDetails",
                        contains("TEST Sumatriptan 50 mg as needed")));

        get(plansUrl(patientA) + "?size=1&page=1", bearer(radiologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(older)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listAccessFollowsTheMatrix() throws Exception {
        createPlan(patientA, "2026-11-01", null);
        get(plansUrl(patientA), bearer(neurologist)).andExpect(status().isOk());
        get(plansUrl(patientA), bearer(radiologist)).andExpect(status().isOk());
        get(plansUrl(patientA), bearer(patientA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        for (String token : new String[]{bearer(patientB), bearer(admin), bearer(mriTech),
                bearer(biomedEngineer), bearer(billingClerk)}) {
            assertStandardError(get(plansUrl(patientA), token), 403);
        }
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(plansUrl(patientA))), 401);
    }

    @Test
    void listForUnknownPatient() throws Exception {
        String unknown = "/api/patients/" + UUID.randomUUID() + "/treatment-plans";
        assertStandardError(get(unknown, bearer(neurologist)), 404);
        assertStandardError(get(unknown, bearer(patientA)), 403);
    }
}
