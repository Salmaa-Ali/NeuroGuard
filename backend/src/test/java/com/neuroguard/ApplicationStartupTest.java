package com.neuroguard;

import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full app against real Postgres: Flyway applies V1+V2 and Hibernate
 * ddl-auto=validate must accept every entity.
 */
class ApplicationStartupTest extends AbstractIntegrationTest {

    @Test
    void contextStartsAndAllPhase2TablesExist() {
        Integer tables = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.tables
                WHERE table_schema = 'public' AND table_name IN (
                  'patient','staff','appointment','diagnosis','treatment_plan','prescription',
                  'service','invoice','invoice_item','payment','mri_order','audit_log',
                  'scanner_device','calibration_session')
                """, Integer.class);
        assertEquals(14, tables);
    }

    @Test
    void protectedUrlReturns401InStandardShape() throws Exception {
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/anything")), 401);
    }

    @Test
    void swaggerAndOpenApiDocsArePublic() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("NeuroGuard API"))
                .andExpect(jsonPath("$.info.version").value("0.2.0"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/staff/login']").exists())
                .andExpect(jsonPath("$.paths['/api/admin/staff']").exists());
    }
}
