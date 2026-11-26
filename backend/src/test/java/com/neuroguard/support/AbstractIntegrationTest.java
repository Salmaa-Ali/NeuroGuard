package com.neuroguard.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neuroguard.patient.entity.Patient;
import com.neuroguard.patient.repository.PatientRepository;
import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.staff.entity.BaseRole;
import com.neuroguard.staff.entity.PermissionScope;
import com.neuroguard.staff.entity.Staff;
import com.neuroguard.staff.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full application against a real Postgres (Testcontainers). Fixtures are recreated before every test:
 * an admin, one staff per permission scope, and two patients (A and B).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class AbstractIntegrationTest {

    public static final String PASSWORD = "Password123!";

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("app.jwt.secret", () -> TokenFactory.TEST_SECRET);
        registry.add("app.bootstrap-admin.email", () -> "");
        registry.add("app.bootstrap-admin.password", () -> "");
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected StaffRepository staffRepository;
    @Autowired protected PatientRepository patientRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected MutableClock testClock;

    protected Staff admin;
    protected Staff neurologist;
    protected Staff radiologist;
    protected Staff mriTech;
    protected Staff biomedEngineer;
    protected Staff billingClerk;
    protected Patient patientA;
    protected Patient patientB;

    @BeforeEach
    void createFixtures() {
        testClock.reset();
        jdbc.execute("TRUNCATE TABLE audit_log, staff, patient CASCADE");
        String hash = passwordEncoder.encode(PASSWORD);
        admin = staff("admin@neuroguard.test", BaseRole.ADMIN, null, hash);
        neurologist = staff("neuro@neuroguard.test", BaseRole.CLINICAL_STAFF, PermissionScope.NEUROLOGIST, hash);
        radiologist = staff("radio@neuroguard.test", BaseRole.CLINICAL_STAFF, PermissionScope.RADIOLOGIST, hash);
        mriTech = staff("mri@neuroguard.test", BaseRole.CLINICAL_STAFF, PermissionScope.MRI_TECHNOLOGIST, hash);
        biomedEngineer = staff("biomed@neuroguard.test", BaseRole.CLINICAL_STAFF, PermissionScope.BIOMEDICAL_ENGINEER, hash);
        billingClerk = staff("billing@neuroguard.test", BaseRole.CLINICAL_STAFF, PermissionScope.BILLING_CLERK, hash);
        patientA = patient("patient.a@neuroguard.test", "P-0001", "Patient A", hash);
        patientB = patient("patient.b@neuroguard.test", "P-0002", "Patient B", hash);
    }

    private Staff staff(String email, BaseRole role, PermissionScope scope, String hash) {
        Staff s = new Staff();
        s.setName(email);
        s.setEmail(email);
        s.setPasswordHash(hash);
        s.setBaseRole(role);
        s.setPermissionScope(scope);
        return staffRepository.save(s);
    }

    private Patient patient(String email, String number, String name, String hash) {
        Patient p = new Patient();
        p.setEmail(email);
        p.setPatientNumber(number);
        p.setName(name);
        p.setPasswordHash(hash);
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        p.setNeurologyHistory("TEST history for " + name);
        return patientRepository.save(p);
    }

    // ---- token helpers ----

    protected String bearer(Staff s) {
        Role role = Role.valueOf(s.getBaseRole().name());
        return "Bearer " + TokenFactory.valid(UserType.STAFF, s.getId(), role, s.getPermissionScope());
    }

    protected String bearer(Patient p) {
        return "Bearer " + TokenFactory.valid(UserType.PATIENT, p.getId(), Role.PATIENT, null);
    }

    // ---- request/assert helpers ----

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions postJson(String url, String authorization, Object body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)
                .header("Authorization", authorization)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions get(String url, String authorization) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                .header("Authorization", authorization));
    }

    protected Map<String, Object> login(String email) {
        return Map.of("email", email, "password", PASSWORD);
    }

    /** Asserts the standard error JSON shape and status. */
    protected void assertStandardError(ResultActions result, int status) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.error").isNotEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    /** No response body may ever expose a password hash. */
    protected void assertNoPasswordHash(ResultActions result) throws Exception {
        MvcResult mvc = result.andReturn();
        String body = mvc.getResponse().getContentAsString().toLowerCase();
        assertFalse(body.contains("password_hash"), "body contains password_hash");
        assertFalse(body.contains("passwordhash"), "body contains passwordHash");
        assertFalse(body.contains("$2a$") || body.contains("$2b$"), "body contains a bcrypt hash");
    }
}
