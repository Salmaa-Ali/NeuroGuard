package com.neuroguard.icd10;

import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class Icd10Test extends AbstractIntegrationTest {

    private static final String URL = "/api/icd10";

    private ResultActions search(String q) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(URL).param("q", q)
                .header("Authorization", bearer(neurologist)));
    }

    @Test
    void searchByCodePrefixIsCaseInsensitiveAndIgnoresDots() throws Exception {
        search("g4").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", contains("G40", "G43")))
                .andExpect(jsonPath("$.totalElements").value(2));
        search("G40.").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", contains("G40")))
                .andExpect(jsonPath("$.content[0].description").value("Epilepsy and recurrent seizures"))
                .andExpect(jsonPath("$.content[0].category").value("G40"));
    }

    @Test
    void searchByDescriptionIsCaseInsensitive() throws Exception {
        search("PARKINSON").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", contains("G20")));
        search("neoplasm").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", contains("C71", "D32")));
    }

    @Test
    void wildcardCharactersAreMatchedLiterally() throws Exception {
        search("%").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        search("_").andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void blankQueryListsAllCodesPagedAndSortedByCode() throws Exception {
        long total = jdbc.queryForObject("SELECT count(*) FROM icd10_code", Long.class);
        get(URL + "?size=3&page=0", bearer(radiologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", contains("C71", "D32", "G20")))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(total))
                .andExpect(jsonPath("$.totalPages").value((int) Math.ceil(total / 3.0)));
        get(URL + "?size=1000", bearer(radiologist)).andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void tooLongQueryReturns400() throws Exception {
        assertStandardError(search("x".repeat(101)), 400);
    }

    @Test
    void getByCodeNormalisesInput() throws Exception {
        get(URL + "/G35", bearer(billingClerk)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("G35"))
                .andExpect(jsonPath("$.description").value("Multiple sclerosis"));
        get(URL + "/i63", bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("I63"));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        assertStandardError(get(URL + "/ZZZ99", bearer(neurologist)), 404);
    }

    @Test
    void everyStaffRoleCanRead() throws Exception {
        for (String token : new String[]{bearer(admin), bearer(neurologist), bearer(radiologist),
                bearer(mriTech), bearer(biomedEngineer), bearer(billingClerk)}) {
            get(URL + "?q=G", token).andExpect(status().isOk());
            get(URL + "/G40", token).andExpect(status().isOk());
        }
    }

    @Test
    void patientGets403() throws Exception {
        assertStandardError(get(URL + "?q=G40", bearer(patientA)), 403);
        assertStandardError(get(URL + "/G40", bearer(patientA)), 403);
        // a patient must not learn whether a code exists either
        assertStandardError(get(URL + "/ZZZ99", bearer(patientA)), 403);
    }

    @Test
    void noTokenGets401() throws Exception {
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(URL)), 401);
        assertStandardError(mockMvc.perform(MockMvcRequestBuilders.get(URL + "/G40")), 401);
    }
}
