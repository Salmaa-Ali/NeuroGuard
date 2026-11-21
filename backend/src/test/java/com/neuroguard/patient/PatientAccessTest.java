package com.neuroguard.patient;

import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PatientAccessTest extends AbstractIntegrationTest {

    private String urlA() {
        return "/api/patients/" + patientA.getId();
    }

    private String urlB() {
        return "/api/patients/" + patientB.getId();
    }

    @Test
    void patientCanReadOwnRecord() throws Exception {
        ResultActions result = get(urlA(), bearer(patientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patientA.getId().toString()))
                .andExpect(jsonPath("$.patientNumber").value("P-0001"))
                .andExpect(jsonPath("$.neurologyHistory").isNotEmpty());
        assertNoPasswordHash(result);
    }

    @Test
    void patientCannotReadAnotherPatient() throws Exception {
        assertStandardError(get(urlB(), bearer(patientA)), 403);
        assertStandardError(get(urlA(), bearer(patientB)), 403);
    }

    @Test
    void neurologistCanReadAnyPatient() throws Exception {
        assertNoPasswordHash(get(urlA(), bearer(neurologist)).andExpect(status().isOk()));
        assertNoPasswordHash(get(urlB(), bearer(neurologist)).andExpect(status().isOk()));
    }

    @Test
    void radiologistMriTechAndBillingClerkCanRead() throws Exception {
        get(urlA(), bearer(radiologist)).andExpect(status().isOk());
        get(urlA(), bearer(mriTech)).andExpect(status().isOk());
        get(urlA(), bearer(billingClerk)).andExpect(status().isOk());
    }

    @Test
    void biomedicalEngineerAndAdminAreForbidden() throws Exception {
        assertStandardError(get(urlA(), bearer(biomedEngineer)), 403);
        assertStandardError(get(urlA(), bearer(admin)), 403);
    }

    @Test
    void unknownPatientReturns404ForAuthorisedStaffButPatientsGet403() throws Exception {
        String unknown = "/api/patients/" + UUID.randomUUID();
        assertStandardError(get(unknown, bearer(neurologist)), 404);
        // a patient must not learn whether someone else's id exists
        assertStandardError(get(unknown, bearer(patientA)), 403);
    }

    @Test
    void malformedIdReturns400() throws Exception {
        assertStandardError(get("/api/patients/not-a-uuid", bearer(neurologist)), 400);
    }
}
