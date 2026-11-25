package com.neuroguard.clinical;

import com.fasterxml.jackson.databind.JsonNode;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Matrix: Neurologist RW, Radiologist R, patient own R; Admin, MRI tech, biomed, billing no access. */
class DiagnosisTest extends AbstractIntegrationTest {

    private String listUrl(Patient p) {
        return "/api/patients/" + p.getId() + "/diagnoses";
    }

    private Map<String, Object> body(String code) {
        Map<String, Object> body = new HashMap<>();
        body.put("icd10Code", code);
        return body;
    }

    /** Creates a diagnosis as the neurologist and returns its id. */
    private String create(Patient p, String code, Instant diagnosedAt) throws Exception {
        Map<String, Object> body = body(code);
        body.put("diagnosedAt", diagnosedAt == null ? null : diagnosedAt.toString());
        String json = postJson(listUrl(p), bearer(neurologist), body)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asText();
    }

    // ---- create ----

    @Test
    void neurologistCreatesDiagnosisAndItIsAudited() throws Exception {
        Map<String, Object> body = body("g40");
        body.put("notes", "  TEST focal seizures  ");
        body.put("staffId", radiologist.getId().toString()); // must be ignored: author comes from the token

        ResultActions result = postJson(listUrl(patientA), bearer(neurologist), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.icd10Code").value("G40"))
                .andExpect(jsonPath("$.icd10Description").value("Epilepsy and recurrent seizures"))
                .andExpect(jsonPath("$.icd10Category").value("G40"))
                .andExpect(jsonPath("$.notes").value("TEST focal seizures"))
                .andExpect(jsonPath("$.staffId").value(neurologist.getId().toString()))
                .andExpect(jsonPath("$.staffName").value(neurologist.getName()))
                .andExpect(jsonPath("$.diagnosedAt").isNotEmpty());
        assertNoPasswordHash(result);

        JsonNode created = objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
        String id = created.get("id").asText();
        result.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/diagnoses/" + id)));
        assertEquals(neurologist.getId().toString(), jdbc.queryForObject(
                "SELECT staff_id::text FROM diagnosis WHERE diagnosis_id = ?::uuid", String.class, id));
        assertEquals(1, jdbc.queryForObject("""
                SELECT count(*) FROM audit_log WHERE action = 'DIAGNOSIS_CREATED' AND actor_type = 'STAFF'
                  AND actor_id = ? AND entity_type = 'DIAGNOSIS' AND entity_id = ?::uuid
                """, Integer.class, neurologist.getId(), id));
    }

    @Test
    void unknownOrMissingIcd10CodeReturns400() throws Exception {
        ResultActions unknown = postJson(listUrl(patientA), bearer(neurologist), body("ZZZ99"));
        assertStandardError(unknown, 400);
        unknown.andExpect(jsonPath("$.fieldErrors[0].field").value("icd10Code"));

        ResultActions missing = postJson(listUrl(patientA), bearer(neurologist), body(null));
        assertStandardError(missing, 400);
        missing.andExpect(jsonPath("$.fieldErrors[0].field").value("icd10Code"));

        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM diagnosis", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM audit_log WHERE action = 'DIAGNOSIS_CREATED'",
                Integer.class));
    }

    @Test
    void futureDiagnosisDateReturns400() throws Exception {
        Map<String, Object> body = body("G40");
        body.put("diagnosedAt", Instant.now().plus(1, ChronoUnit.DAYS).toString());
        ResultActions result = postJson(listUrl(patientA), bearer(neurologist), body);
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.fieldErrors[0].field").value("diagnosedAt"));
    }

    @Test
    void unknownPatientReturns404ForNeurologist() throws Exception {
        assertStandardError(postJson("/api/patients/" + UUID.randomUUID() + "/diagnoses",
                bearer(neurologist), body("G40")), 404);
    }

    @Test
    void onlyNeurologistMayCreate() throws Exception {
        for (String token : new String[]{bearer(radiologist), bearer(admin), bearer(mriTech),
                bearer(biomedEngineer), bearer(billingClerk), bearer(patientA)}) {
            assertStandardError(postJson(listUrl(patientA), token, body("G40")), 403);
        }
        assertStandardError(postJson(listUrl(patientA), body("G40")), 401);
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM diagnosis", Integer.class));
    }

    // ---- list ----

    @Test
    void listIsNewestFirstPagedAndOnlyForThatPatient() throws Exception {
        Instant now = Instant.now();
        String oldest = create(patientA, "G43", now.minus(30, ChronoUnit.DAYS));
        String newest = create(patientA, "G40", now.minus(1, ChronoUnit.DAYS));
        String middle = create(patientA, "G35", now.minus(10, ChronoUnit.DAYS));
        create(patientB, "G20", now.minus(5, ChronoUnit.DAYS));

        get(listUrl(patientA), bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(newest, middle, oldest)))
                .andExpect(jsonPath("$.totalElements").value(3));
        get(listUrl(patientA) + "?size=2&page=1", bearer(radiologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", contains(oldest)))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void listAccessFollowsTheMatrix() throws Exception {
        create(patientA, "G40", null);
        get(listUrl(patientA), bearer(neurologist)).andExpect(status().isOk());
        get(listUrl(patientA), bearer(radiologist)).andExpect(status().isOk());
        get(listUrl(patientA), bearer(patientA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        for (String token : new String[]{bearer(patientB), bearer(admin), bearer(mriTech),
                bearer(biomedEngineer), bearer(billingClerk)}) {
            assertStandardError(get(listUrl(patientA), token), 403);
        }
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(listUrl(patientA))), 401);
    }

    @Test
    void listForUnknownPatient() throws Exception {
        String unknown = "/api/patients/" + UUID.randomUUID() + "/diagnoses";
        assertStandardError(get(unknown, bearer(neurologist)), 404);
        assertStandardError(get(unknown, bearer(patientA)), 403);
    }

    // ---- get one ----

    @Test
    void getOneFollowsTheMatrixWithOwnershipCheckedInService() throws Exception {
        String url = "/api/diagnoses/" + create(patientA, "C71", null);

        get(url, bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.icd10Description").value("Malignant neoplasm of brain"));
        get(url, bearer(radiologist)).andExpect(status().isOk());
        get(url, bearer(patientA)).andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(patientA.getId().toString()));

        assertStandardError(get(url, bearer(patientB)), 403);
        for (String token : new String[]{bearer(admin), bearer(mriTech), bearer(biomedEngineer), bearer(billingClerk)}) {
            assertStandardError(get(url, token), 403);
        }
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(url)), 401);
    }

    @Test
    void unknownDiagnosisIs404ForStaffAnd403ForPatients() throws Exception {
        String url = "/api/diagnoses/" + UUID.randomUUID();
        assertStandardError(get(url, bearer(neurologist)), 404);
        assertStandardError(get(url, bearer(patientA)), 403);
        assertStandardError(get("/api/diagnoses/not-a-uuid", bearer(neurologist)), 400);
    }
}
