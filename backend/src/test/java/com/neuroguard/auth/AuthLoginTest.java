package com.neuroguard.auth;

import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthLoginTest extends AbstractIntegrationTest {

    @Autowired
    JwtDecoder jwtDecoder;

    @Test
    void adminStaffLoginSucceedsAndTokenHasExpectedClaims() throws Exception {
        ResultActions result = postJson("/api/auth/staff/login", login(admin.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.userType").value("STAFF"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
        assertNoPasswordHash(result);

        String token = objectMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("accessToken").asText();
        Jwt jwt = jwtDecoder.decode(token);
        assertEquals(admin.getId().toString(), jwt.getSubject());
        assertEquals("STAFF", jwt.getClaimAsString("user_type"));
        assertEquals("ADMIN", jwt.getClaimAsString("role"));
        assertNull(jwt.getClaimAsString("perm_scope"));
        assertEquals("neuroguard", jwt.getClaimAsString("iss"));
        assertEquals(3600, jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond());
    }

    @Test
    void clinicalStaffLoginIncludesScope() throws Exception {
        ResultActions result = postJson("/api/auth/staff/login", login(neurologist.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CLINICAL_STAFF"))
                .andExpect(jsonPath("$.scope").value("NEUROLOGIST"));
        String token = objectMapper.readTree(result.andReturn().getResponse().getContentAsString())
                .get("accessToken").asText();
        assertEquals("NEUROLOGIST", jwtDecoder.decode(token).getClaimAsString("perm_scope"));
    }

    @Test
    void patientLoginSucceeds() throws Exception {
        ResultActions result = postJson("/api/auth/patient/login", login(patientA.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("PATIENT"))
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        assertNoPasswordHash(result);
    }

    @Test
    void emailIsCaseInsensitive() throws Exception {
        postJson("/api/auth/staff/login", login("ADMIN@NeuroGuard.test"))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailReturnSameMessage() throws Exception {
        ResultActions wrongPassword = postJson("/api/auth/staff/login",
                Map.of("email", admin.getEmail(), "password", "not-the-password"));
        assertStandardError(wrongPassword, 401);
        wrongPassword.andExpect(jsonPath("$.message").value("Invalid email or password"));

        ResultActions unknownEmail = postJson("/api/auth/staff/login",
                Map.of("email", "nobody@neuroguard.test", "password", PASSWORD));
        assertStandardError(unknownEmail, 401);
        unknownEmail.andExpect(jsonPath("$.message").value("Invalid email or password"));

        assertEquals(
                objectMapper.readTree(wrongPassword.andReturn().getResponse().getContentAsString()).get("message"),
                objectMapper.readTree(unknownEmail.andReturn().getResponse().getContentAsString()).get("message"));
    }

    @Test
    void patientEndpointRejectsStaffCredentialsAndViceVersa() throws Exception {
        assertStandardError(postJson("/api/auth/patient/login", login(admin.getEmail())), 401);
        assertStandardError(postJson("/api/auth/staff/login", login(patientA.getEmail())), 401);
    }

    @Test
    void invalidLoginBodyReturns400WithFieldErrors() throws Exception {
        postJson("/api/auth/staff/login", Map.of("email", "not-an-email", "password", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isNotEmpty());
    }

    @Test
    void loginsAreAuditedWithoutStoringPasswords() throws Exception {
        postJson("/api/auth/staff/login", login(admin.getEmail())).andExpect(status().isOk());
        postJson("/api/auth/staff/login", Map.of("email", admin.getEmail(), "password", "wrong-password-xyz"));
        postJson("/api/auth/patient/login", Map.of("email", "ghost@neuroguard.test", "password", "wrong-password-xyz"));

        Integer success = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action='LOGIN_SUCCESS' AND actor_type='STAFF' AND actor_id=?",
                Integer.class, admin.getId());
        Integer failedKnown = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action='LOGIN_FAILED' AND actor_type='STAFF' AND actor_id=?",
                Integer.class, admin.getId());
        Integer failedUnknown = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action='LOGIN_FAILED' AND actor_type='PATIENT' AND actor_id IS NULL",
                Integer.class);
        assertEquals(1, success);
        assertEquals(1, failedKnown);
        assertEquals(1, failedUnknown);

        Integer leaked = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action::text ILIKE '%wrong-password%' "
                        + "OR coalesce(entity_type,'') ILIKE '%wrong-password%'", Integer.class);
        assertEquals(0, leaked);
    }
}
