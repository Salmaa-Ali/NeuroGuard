package com.neuroguard.config;

import com.neuroguard.common.ApiErrorWriter;
import com.neuroguard.common.GlobalExceptionHandler;
import com.neuroguard.common.JsonAccessDeniedHandler;
import com.neuroguard.common.JsonAuthenticationEntryPoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Web-layer only (no database / Docker needed). */
@WebMvcTest(ProtectedEndpointTest.PingController.class)
@Import({SecurityConfig.class, ApiErrorWriter.class, JsonAuthenticationEntryPoint.class,
        JsonAccessDeniedHandler.class, GlobalExceptionHandler.class, ProtectedEndpointTest.PingController.class})
class ProtectedEndpointTest {

    @RestController
    static class PingController {
        @GetMapping("/api/v1/patients")
        String ping() {
            return "pong";
        }
    }

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    JwtDecoder jwtDecoder;

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
