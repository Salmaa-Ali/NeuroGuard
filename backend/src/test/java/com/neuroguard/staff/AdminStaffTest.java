package com.neuroguard.staff;

import com.neuroguard.staff.entity.Staff;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminStaffTest extends AbstractIntegrationTest {

    private static final String URL = "/api/admin/staff";

    private Map<String, Object> newStaff(String email, String baseRole, String scope) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "New Person");
        body.put("email", email);
        body.put("password", "SuperSecret1");
        body.put("baseRole", baseRole);
        body.put("permissionScope", scope);
        return body;
    }

    @Test
    void adminCanCreateClinicalStaff() throws Exception {
        ResultActions result = postJson(URL, bearer(admin), newStaff("New.Doc@neuroguard.test", "CLINICAL_STAFF", "RADIOLOGIST"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.email").value("new.doc@neuroguard.test"))
                .andExpect(jsonPath("$.baseRole").value("CLINICAL_STAFF"))
                .andExpect(jsonPath("$.permissionScope").value("RADIOLOGIST"));
        assertNoPasswordHash(result);

        // password is stored hashed and the new account can log in
        Staff saved = staffRepository.findByEmail("new.doc@neuroguard.test").orElseThrow();
        assertTrue(saved.getPasswordHash().startsWith("$2"));
        postJson("/api/auth/staff/login", Map.of("email", "new.doc@neuroguard.test", "password", "SuperSecret1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.scope").value("RADIOLOGIST"));

        Integer audited = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action='STAFF_CREATED' AND actor_id=? AND entity_id=?",
                Integer.class, admin.getId(), saved.getId());
        assertEquals(1, audited);
    }

    @Test
    void adminCanCreateAdminWithoutScope() throws Exception {
        postJson(URL, bearer(admin), newStaff("second.admin@neuroguard.test", "ADMIN", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.baseRole").value("ADMIN"));
    }

    @Test
    void neurologistAndPatientGet403() throws Exception {
        Map<String, Object> body = newStaff("x@neuroguard.test", "ADMIN", null);
        assertStandardError(postJson(URL, bearer(neurologist), body), 403);
        assertStandardError(postJson(URL, bearer(patientA), body), 403);
        assertStandardError(get(URL, bearer(biomedEngineer)), 403);
        assertStandardError(get(URL + "/" + admin.getId(), bearer(patientA)), 403);
    }

    @Test
    void noTokenGets401() throws Exception {
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(URL)), 401);
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        assertStandardError(postJson(URL, bearer(admin), newStaff(neurologist.getEmail().toUpperCase(), "ADMIN", null)), 409);
    }

    @Test
    void clinicalStaffWithoutScopeReturns400() throws Exception {
        ResultActions result = postJson(URL, bearer(admin), newStaff("noscope@neuroguard.test", "CLINICAL_STAFF", null));
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.fieldErrors[0].field").value("permissionScope"));
    }

    @Test
    void adminWithScopeReturns400() throws Exception {
        assertStandardError(postJson(URL, bearer(admin), newStaff("adm@neuroguard.test", "ADMIN", "NEUROLOGIST")), 400);
    }

    @Test
    void shortPasswordAndBadEmailReturn400() throws Exception {
        Map<String, Object> body = newStaff("bad-email", "ADMIN", null);
        body.put("password", "short");
        ResultActions result = postJson(URL, bearer(admin), body);
        assertStandardError(result, 400);
        result.andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void invalidEnumValueReturns400() throws Exception {
        assertStandardError(postJson(URL, bearer(admin), newStaff("e@neuroguard.test", "SUPERUSER", null)), 400);
    }

    @Test
    void listIsPagedAndNeverExposesHashes() throws Exception {
        ResultActions result = get(URL + "?size=3&page=0&sortBy=email", bearer(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(6))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.content[0].email").value("admin@neuroguard.test"));
        assertNoPasswordHash(result);
        assertStandardError(get(URL + "?sortBy=passwordHash", bearer(admin)), 400);
    }

    @Test
    void getByIdAndUnknownId() throws Exception {
        ResultActions result = get(URL + "/" + neurologist.getId(), bearer(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(neurologist.getEmail()));
        assertNoPasswordHash(result);
        assertStandardError(get(URL + "/" + java.util.UUID.randomUUID(), bearer(admin)), 404);
    }

    @Test
    void adminCanChangeRoleAndItIsAudited() throws Exception {
        ResultActions result = mockMvc.perform(MockMvcRequestBuilders.patch(URL + "/" + radiologist.getId() + "/role")
                        .header("Authorization", bearer(admin)).contentType("application/json")
                        .content("{\"baseRole\":\"CLINICAL_STAFF\",\"permissionScope\":\"NEUROLOGIST\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionScope").value("NEUROLOGIST"));
        assertNoPasswordHash(result);
        Integer audited = jdbc.queryForObject(
                "SELECT count(*) FROM audit_log WHERE action='STAFF_ROLE_CHANGED' AND entity_id=?",
                Integer.class, radiologist.getId());
        assertEquals(1, audited);
    }

    @Test
    void changeRoleEnforcesScopeRulesAndAdminOnly() throws Exception {
        String path = URL + "/" + radiologist.getId() + "/role";
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.patch(path)
                .header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"baseRole\":\"CLINICAL_STAFF\"}")), 400);
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.patch(path)
                .header("Authorization", bearer(neurologist)).contentType("application/json")
                .content("{\"baseRole\":\"ADMIN\"}")), 403);
    }

    @Test
    void lastAdminCannotBeDemoted() throws Exception {
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.patch(URL + "/" + admin.getId() + "/role")
                .header("Authorization", bearer(admin)).contentType("application/json")
                .content("{\"baseRole\":\"CLINICAL_STAFF\",\"permissionScope\":\"NEUROLOGIST\"}")), 409);
    }
}
