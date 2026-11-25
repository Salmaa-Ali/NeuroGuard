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

/** V4: diagnosis.notes (optional) and prescription.staff_id (required, FK to staff). */
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
    void prescriptionStaffIdIsRequiredWithForeignKeyToStaff() {
        String type = jdbc.queryForObject("""
                SELECT data_type || ':' || is_nullable FROM information_schema.columns
                WHERE table_name = 'prescription' AND column_name = 'staff_id'
                """, String.class);
        assertEquals("uuid:NO", type);
        Integer fk = jdbc.queryForObject("""
                SELECT count(*) FROM information_schema.referential_constraints rc
                JOIN information_schema.key_column_usage k ON k.constraint_name = rc.constraint_name
                WHERE k.table_name = 'prescription' AND k.column_name = 'staff_id'
                """, Integer.class);
        assertEquals(1, fk);
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
