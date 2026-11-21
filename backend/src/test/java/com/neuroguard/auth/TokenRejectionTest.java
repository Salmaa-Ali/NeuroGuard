package com.neuroguard.auth;

import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.staff.entity.PermissionScope;
import com.neuroguard.support.AbstractIntegrationTest;
import com.neuroguard.support.TokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TokenRejectionTest extends AbstractIntegrationTest {

    private String url() {
        return "/api/patients/" + patientA.getId();
    }

    private void assertRejected(String authorization) throws Exception {
        assertStandardError(get(url(), authorization), 401);
    }

    @Test
    void noTokenReturns401() throws Exception {
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(url())), 401);
    }

    @Test
    void garbageTokenReturns401() throws Exception {
        assertRejected("Bearer this-is-not-a-jwt");
        assertRejected("Bearer a.b.c");
    }

    @Test
    void expiredTokenReturns401() throws Exception {
        assertRejected("Bearer " + TokenFactory.expired(UserType.PATIENT, patientA.getId(), Role.PATIENT, null));
    }

    @Test
    void wrongSignatureReturns401() throws Exception {
        assertRejected("Bearer " + TokenFactory.signedWithWrongKey(UserType.PATIENT, patientA.getId(), Role.PATIENT, null));
    }

    @Test
    void tamperedPayloadReturns401() throws Exception {
        assertRejected("Bearer " + TokenFactory.tamperedPayload(patientA.getId()));
    }

    @Test
    void algNoneReturns401() throws Exception {
        assertRejected("Bearer " + TokenFactory.algNone());
    }

    @Test
    void wrongIssuerReturns401() throws Exception {
        assertRejected("Bearer " + TokenFactory.wrongIssuer(UserType.PATIENT, patientA.getId(), Role.PATIENT, null));
    }

    @Test
    void validSignatureButInconsistentClaimsReturns401() throws Exception {
        // CLINICAL_STAFF without perm_scope is not something the server ever issues
        assertRejected("Bearer " + TokenFactory.valid(UserType.STAFF, neurologist.getId(), Role.CLINICAL_STAFF, null));
        // a patient token claiming an admin role
        assertRejected("Bearer " + TokenFactory.valid(UserType.PATIENT, patientA.getId(), Role.ADMIN, null));
        // a scope on a patient
        assertRejected("Bearer " + TokenFactory.valid(UserType.PATIENT, patientA.getId(), Role.PATIENT,
                PermissionScope.NEUROLOGIST));
    }

    @Test
    void nonBearerSchemeReturns401() throws Exception {
        assertRejected("Basic dXNlcjpwYXNz");
    }

    @Test
    void validTokenIsAccepted() throws Exception {
        get(url(), bearer(patientA)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(patientA.getId().toString()));
    }
}
