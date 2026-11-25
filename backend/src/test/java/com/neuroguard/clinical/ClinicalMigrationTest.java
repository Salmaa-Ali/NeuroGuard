package com.neuroguard.clinical;

import com.neuroguard.clinical.entity.Diagnosis;
import com.neuroguard.clinical.repository.DiagnosisRepository;
import com.neuroguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** V4: diagnosis.notes exists, is optional, and is mapped by the entity. */
class ClinicalMigrationTest extends AbstractIntegrationTest {

    @Autowired
    private DiagnosisRepository diagnosisRepository;

    @Test
    void notesColumnIsNullableText() {
        String type = jdbc.queryForObject("""
                SELECT data_type || ':' || is_nullable FROM information_schema.columns
                WHERE table_name = 'diagnosis' AND column_name = 'notes'
                """, String.class);
        assertEquals("text:YES", type);
    }

    @Test
    void entityStoresNotesAndAllowsNone() {
        Diagnosis withNotes = diagnosis("G40", "TEST seizures since childhood");
        Diagnosis withoutNotes = diagnosis("G43", null);

        assertEquals("TEST seizures since childhood",
                diagnosisRepository.findById(withNotes.getId()).orElseThrow().getNotes());
        assertNull(diagnosisRepository.findById(withoutNotes.getId()).orElseThrow().getNotes());
    }

    private Diagnosis diagnosis(String code, String notes) {
        Diagnosis d = new Diagnosis();
        d.setPatient(patientA);
        d.setStaff(neurologist);
        d.setIcd10Code(code);
        d.setDiagnosedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        d.setNotes(notes);
        return diagnosisRepository.saveAndFlush(d);
    }
}
