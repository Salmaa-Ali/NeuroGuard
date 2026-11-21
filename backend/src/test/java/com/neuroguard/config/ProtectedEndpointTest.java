package com.neuroguard.config;

import com.neuroguard.common.ApiErrorWriter;
import com.neuroguard.common.GlobalExceptionHandler;
import com.neuroguard.common.JsonAccessDeniedHandler;
import com.neuroguard.common.JsonAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer only (no database / Docker needed). */
@WebMvcTest
@Import({SecurityConfig.class, ApiErrorWriter.class, JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class, GlobalExceptionHandler.class})
class ProtectedEndpointTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void protectedUrlWithoutCredentialsReturns401InStandardJsonShape() throws Exception {
        mockMvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/patients"))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void healthEndpointIsPublicPath() throws Exception {
        // No actuator in the web slice, so the request passes security and ends as 404, not 401.
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isNotFound());
    }
}
