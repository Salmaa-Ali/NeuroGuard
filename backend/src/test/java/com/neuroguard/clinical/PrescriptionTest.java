package com.neuroguard.clinical;

import com.neuroguard.patient.entity.Patient;
import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Prescriptions live under a plan; same matrix as diagnoses, "own" checked through the plan's patient. */
class PrescriptionTest extends AbstractIntegrationTest {

    private String createPlan(Patient p) throws Exception {
        String json = postJson(TreatmentPlanTest.plansUrl(p), bearer(neurologist),
                TreatmentPlanTest.plan("TEST plan", "2026-11-01", null))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    private static String url(String planId) {
        return "/api/treatment-plans/" + planId + "/prescriptions";
    }

    private static Map<String, Object> rx(String details) {
        return Map.of("medicationDetails", details);
    }

    @Test
    void neurologistAddsPrescriptionAndItIsAudited() throws Exception {
        String planId = createPlan(patientA);
        String json = postJson(url(planId), bearer(neurologist), rx("  TEST Levetiracetam 500 mg twice daily "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.planId").value(planId))
                .andExpect(jsonPath("$.medicationDetails").value("TEST Levetiracetam 500 mg twice daily"))
                .andExpect(jsonPath("$.staffId").value(neurologist.getId().toString()))
                .andExpect(jsonPath("$.staffName").value(neurologist.getName()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(json).get("id").asText();

        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'PRESCRIPTION_CREATED' AND actor_id = ?
                  AND entity_type = 'PRESCRIPTION' AND entity_id = ?::uuid
                """, Integer.class, neurologist.getId(), id));
    }

    @Test
    void prescriberIsTheCallerNotThePlanAuthor() throws Exception {
        String planId = createPlan(patientA); // authored by the fixture neurologist
        Staff colleague = new Staff();
        colleague.setName("TEST Second Neurologist");
        colleague.setEmail("neuro2@neuroguard.test");
        colleague.setPasswordHash(neurologist.getPasswordHash());
        colleague.setBaseRole(BaseRole.CLINICAL_STAFF);
        colleague.setPermissionScope(PermissionScope.NEUROLOGIST);
        colleague = staffRepository.save(colleague);

        postJson(url(planId), bearer(colleague), Map.of("medicationDetails", "TEST Topiramate 25 mg nightly",
                "staffId", neurologist.getId().toString())) // ignored: prescriber comes from the token
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.staffId").value(colleague.getId().toString()));

        get(TreatmentPlanTest.plansUrl(patientA), bearer(patientA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].staffId").value(neurologist.getId().toString()))
                .andExpect(jsonPath("$.content[0].prescriptions[0].staffName").value("TEST Second Neurologist"));
        get(url(planId), bearer(radiologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].staffId").value(colleague.getId().toString()));
    }

    @Test
    void blankMedicationReturns400AndUnknownPlanReturns404() throws Exception {
        String planId = createPlan(patientA);
        ResultActions blank = postJson(url(planId), bearer(neurologist), rx("   "));
        assertStandardError(blank, 400);
        blank.andExpect(jsonPath("$.fieldErrors[0].field").value("medicationDetails"));

        assertStandardError(postJson(url(UUID.randomUUID().toString()), bearer(neurologist), rx("TEST x")), 404);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM prescription", Integer.class));
    }

    @Test
    void onlyNeurologistMayPrescribe() throws Exception {
        String planId = createPlan(patientA);
        for (String token : new String[]{bearer(radiologist), bearer(admin), bearer(mriTech),
                bearer(biomedEngineer), bearer(billingClerk), bearer(patientA)}) {
            assertStandardError(postJson(url(planId), token, rx("TEST x")), 403);
        }
        assertStandardError(postJson(url(planId), rx("TEST x")), 401);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM prescription", Integer.class));
    }

    @Test
    void listByPlanIsOldestFirstAndFollowsTheMatrix() throws Exception {
        String planId = createPlan(patientA);
        postJson(url(planId), bearer(neurologist), rx("TEST first")).andExpect(status().isCreated());
        postJson(url(planId), bearer(neurologist), rx("TEST second")).andExpect(status().isCreated());

        get(url(planId), bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].medicationDetails", contains("TEST first", "TEST second")));
        get(url(planId), bearer(radiologist)).andExpect(status().isOk());
        get(url(planId), bearer(patientA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // ownership is checked in the service through the plan's patient
        assertStandardError(get(url(planId), bearer(patientB)), 403);
        for (String token : new String[]{bearer(admin), bearer(mriTech), bearer(biomedEngineer), bearer(billingClerk)}) {
            assertStandardError(get(url(planId), token), 403);
        }
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(url(planId))), 401);
    }

    @Test
    void unknownPlanIs404ForStaffAnd403ForPatients() throws Exception {
        String unknown = url(UUID.randomUUID().toString());
        assertStandardError(get(unknown, bearer(neurologist)), 404);
        assertStandardError(get(unknown, bearer(patientA)), 403);
        assertStandardError(get(url("not-a-uuid"), bearer(neurologist)), 400);
    }
}
