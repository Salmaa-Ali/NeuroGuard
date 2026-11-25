package com.neuroguard.icd10;

import com.neuroguard.icd10.service.Icd10ImportRunner;
import com.neuroguard.icd10.service.Icd10ImportService;
import com.neuroguard.icd10.service.Icd10ImportService.ImportResult;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sample file (src/test/resources/icd10/order-sample.txt): 8 non-blank lines =
 * G20, G40 unchanged (same as the V3 starter set); G30 updated (its description is changed before
 * the import); G400, G40901, G43909 new; one malformed line and one repeated code skipped.
 */
class Icd10ImportTest extends AbstractIntegrationTest {

    private static final String SAMPLE = "/icd10/order-sample.txt";

    @Autowired
    private Icd10ImportService importService;
    @Autowired
    private ApplicationContext context;

    @AfterEach
    void removeImportedCodes() {
        jdbc.update("DELETE FROM icd10_code WHERE code IN ('G400', 'G40901', 'G43909')");
        jdbc.update("UPDATE icd10_code SET description = 'Alzheimer''s disease' WHERE code = 'G30'");
    }

    private ImportResult importSample() throws IOException {
        try (InputStream in = getClass().getResourceAsStream(SAMPLE)) {
            return importService.importStream(in);
        }
    }

    @Test
    void importsNewCodesUpdatesChangedOnesAndReportsCounts() throws Exception {
        jdbc.update("UPDATE icd10_code SET description = 'old text' WHERE code = 'G30'");

        assertEquals(new ImportResult(8, 3, 1, 2, 2), importSample());

        assertEquals("Alzheimer's disease",
                jdbc.queryForObject("SELECT description FROM icd10_code WHERE code = 'G30'", String.class));
        assertEquals("G40", jdbc.queryForObject("SELECT category FROM icd10_code WHERE code = 'G40901'", String.class));
        // the long description is used, and the first occurrence of a repeated code wins
        assertEquals("Migraine, unspecified, not intractable, without status migrainosus",
                jdbc.queryForObject("SELECT description FROM icd10_code WHERE code = 'G43909'", String.class));
        // imported codes are served by the API like the starter set
        get("/api/icd10/G40.901", bearer(neurologist)).andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Epilepsy, unspecified, not intractable, with status epilepticus"));
    }

    @Test
    void secondRunChangesNothing() throws Exception {
        importSample();
        long rowsAfterFirst = jdbc.queryForObject("SELECT count(*) FROM icd10_code", Long.class);

        assertEquals(new ImportResult(8, 0, 0, 6, 2), importSample());
        assertEquals(rowsAfterFirst, jdbc.queryForObject("SELECT count(*) FROM icd10_code", Long.class));
    }

    @Test
    void codesMissingFromTheFileAreKept() throws Exception {
        importSample();
        // G35 is in the starter set but not in the sample file
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM icd10_code WHERE code = 'G35'", Integer.class));
    }

    @Test
    void runnerIsOnlyActiveUnderItsProfile() {
        assertTrue(context.getBeansOfType(Icd10ImportRunner.class).isEmpty());
    }

    @Test
    void runnerRefusesMissingOrUnreadableFile() {
        assertThrows(IllegalStateException.class, () -> new Icd10ImportRunner(importService, "").run());
        Path missing = Path.of("does-not-exist", "icd10cm_order.txt");
        assertThrows(IllegalStateException.class,
                () -> new Icd10ImportRunner(importService, missing.toString()).run());
    }
}
