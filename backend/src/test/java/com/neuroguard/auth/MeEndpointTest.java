package com.neuroguard.auth;

import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.support.AbstractIntegrationTest;
import com.neuroguard.support.TokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeEndpointTest extends AbstractIntegrationTest {

    @Test
    void clinicalStaffSeesOwnNameEmailRoleAndScope() throws Exception {
        neurologist.setName("Dr. Nora Neuro");
        staffRepository.save(neurologist);
        ResultActions result = get("/api/me", bearer(neurologist))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(neurologist.getId().toString()))
                .andExpect(jsonPath("$.name").value("Dr. Nora Neuro"))
                .andExpect(jsonPath("$.email").value(neurologist.getEmail()))
                .andExpect(jsonPath("$.userType").value("STAFF"))
                .andExpect(jsonPath("$.role").value("CLINICAL_STAFF"))
                .andExpect(jsonPath("$.scope").value("NEUROLOGIST"));
        assertNoPasswordHash(result);
    }

    @Test
    void adminAndPatientCanCallIt() throws Exception {
        get("/api/me", bearer(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        ResultActions result = get("/api/me", bearer(patientA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Patient A"))
                .andExpect(jsonPath("$.userType").value("PATIENT"))
                .andExpect(jsonPath("$.role").value("PATIENT"));
        assertNoPasswordHash(result);
    }

    @Test
    void noTokenReturns401() throws Exception {
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get("/api/me")), 401);
    }

    @Test
    void validTokenForDeletedAccountReturns401() throws Exception {
        String ghost = "Bearer " + TokenFactory.valid(UserType.PATIENT, UUID.randomUUID(), Role.PATIENT, null);
        assertStandardError(get("/api/me", ghost), 401);
    }
}
