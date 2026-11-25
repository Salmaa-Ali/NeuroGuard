package com.neuroguard.icd10;

import com.neuroguard.icd10.entity.Icd10Code;
import com.neuroguard.icd10.repository.Icd10CodeRepository;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** V3: starter codes are present, the entity maps them, and the new foreign keys are enforced. */
class Icd10MigrationTest extends AbstractIntegrationTest {

    @Autowired
    private Icd10CodeRepository icd10CodeRepository;

    @Test
    void starterSetIsLoadedWithCategories() {
        List<String> codes = jdbc.queryForList(
                "SELECT code FROM icd10_code WHERE code IN ('G40','G35','G43','I63','C71','D32','G20','G30') "
                        + "AND category = code", String.class);
        assertEquals(8, codes.size());

        Icd10Code g20 = icd10CodeRepository.findById("G20").orElseThrow();
        assertEquals("Parkinson's disease", g20.getDescription());
        assertEquals("G20", g20.getCategory());
    }

    @Test
    void diagnosisWithUnknownCodeIsRejectedByForeignKey() {
        String insert = "INSERT INTO diagnosis (patient_id, staff_id, icd10_code) VALUES (?, ?, ?)";
        jdbc.update(insert, patientA.getId(), neurologist.getId(), "G40");
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update(insert, patientA.getId(), neurologist.getId(), "ZZZ99"));
    }

    @Test
    void invoiceItemWithUnknownCodeIsRejectedByForeignKey() {
        UUID serviceId = jdbc.queryForObject(
                "INSERT INTO service (name, price) VALUES ('TEST fk service', 100.00) RETURNING service_id", UUID.class);
        UUID invoiceId = jdbc.queryForObject(
                "INSERT INTO invoice (patient_id) VALUES (?) RETURNING invoice_id", UUID.class, patientA.getId());
        try {
            String insert = "INSERT INTO invoice_item (invoice_id, service_id, icd10_code, unit_price, line_total) "
                    + "VALUES (?, ?, ?, 100.00, 100.00)";
            jdbc.update(insert, invoiceId, serviceId, "G43");
            jdbc.update(insert, invoiceId, serviceId, null);
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update(insert, invoiceId, serviceId, "ZZZ99"));
        } finally {
            // service is not covered by the fixture TRUNCATE; CASCADE also removes the invoice items
            jdbc.update("TRUNCATE TABLE service CASCADE");
        }
    }
}
